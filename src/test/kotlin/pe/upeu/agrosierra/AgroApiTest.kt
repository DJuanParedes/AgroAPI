package pe.upeu.agrosierra

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import java.time.Instant
import java.util.Date

@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:agrotest;DB_CLOSE_DELAY=-1", "agro.jwt-secret=clave-de-prueba-con-mas-de-treinta-y-dos-bytes"])
@AutoConfigureMockMvc
class AgroApiTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var jdbc: JdbcTemplate
    private fun token(usuario: String = "tecnico1"): String {
        val res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""{"usuario":"$usuario","contrasena":"Agro2026*"}""")).andReturn().response
        assertEquals(200, res.status)
        return mapper.readTree(res.contentAsString)["token"].asText()
    }
    private fun labor(lote: Int = 2, tipo: String = "RIEGO", cantidad: Double = 2.0) =
        """{"loteId":$lote,"tipo":"$tipo","fecha":"2026-10-27","cantidad":$cantidad,"observacion":"Prueba"}"""

    @Test fun `login valida la contrasena y firma un token de una hora`() {
        val t = token()
        val claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor("clave-de-prueba-con-mas-de-treinta-y-dos-bytes".toByteArray())).build().parseSignedClaims(t).payload
        assertEquals(3600000L, claims.expiration.time - claims.issuedAt.time)
        assertTrue(claims.containsKey("privilegios"))
        val r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""{"usuario":"tecnico1","contrasena":"incorrecta"}""")).andReturn().response
        assertEquals(401, r.status)
        assertTrue(jdbc.queryForObject("SELECT contrasena FROM usuario WHERE usuario='tecnico1'", String::class.java)!!.startsWith("\$2"))
    }
    @Test fun `todas las rutas protegidas rechazan token ausente`() {
        listOf("/api/v1/lotes", "/api/v1/lotes/1", "/api/v1/lotes/1/labores", "/api/v1/usuarios/me").forEach {
            assertEquals(401, mvc.perform(get(it)).andReturn().response.status)
        }
        assertEquals(401, mvc.perform(post("/api/v1/labores").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response.status)
        assertEquals(401, mvc.perform(put("/api/v1/labores/1").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response.status)
        assertEquals(401, mvc.perform(delete("/api/v1/labores/1")).andReturn().response.status)
        assertEquals(401, mvc.perform(post("/api/v1/lotes/1/cerrar-campania")).andReturn().response.status)
    }
    @Test fun `token alterado y vencido responden 401`() {
        assertEquals(401, mvc.perform(get("/api/v1/lotes").header("Authorization", "Bearer ${token()}alterado")).andReturn().response.status)
        val key = Keys.hmacShaKeyFor("clave-de-prueba-con-mas-de-treinta-y-dos-bytes".toByteArray())
        val vencido = Jwts.builder().subject("1").issuer("AgroAPI").expiration(Date.from(Instant.now().minusSeconds(10))).signWith(key).compact()
        assertEquals(401, mvc.perform(get("/api/v1/lotes").header("Authorization", "Bearer $vencido")).andReturn().response.status)
    }
    @Test fun `privilegios se verifican en el servicio HTTP`() {
        assertEquals(403, mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer ${token("admin")}").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response.status)
        assertEquals(403, mvc.perform(post("/api/v1/lotes/1/cerrar-campania").header("Authorization", "Bearer ${token()}")).andReturn().response.status)
        assertEquals(403, mvc.perform(delete("/api/v1/labores/1").header("Authorization", "Bearer ${token()}")).andReturn().response.status)
    }
    @Test fun `paginacion y filtros respetan el contrato`() {
        val res = mvc.perform(get("/api/v1/lotes?pagina=0&tamanio=3&valle=Mantaro").header("Authorization", "Bearer ${token()}")).andReturn().response
        assertEquals(200, res.status)
        val page = mapper.readTree(res.contentAsString)
        assertTrue(page.has("contenido")); assertEquals(3, page["tamanio"].asInt())
        page["contenido"].forEach { assertEquals("Mantaro", it["valle"].asText()) }
    }
    @Test fun `reglas RN01 RN03 y RN04 devuelven conflicto o validacion`() {
        val tecnico = token(); val jefe = token("jefe1")
        assertEquals(409, mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer $tecnico").contentType(MediaType.APPLICATION_JSON).content(labor(7))).andReturn().response.status)
        assertEquals(409, mvc.perform(post("/api/v1/lotes/2/cerrar-campania").header("Authorization", "Bearer $jefe")).andReturn().response.status)
        val r = mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer $tecnico").contentType(MediaType.APPLICATION_JSON).content(labor(tipo = "FUMIGACION", cantidad = 999.0))).andReturn().response
        assertEquals(400, r.status); assertTrue(mapper.readTree(r.contentAsString)["validationErrors"].has("cantidad"))
    }
    @Test fun `CRUD persiste e idempotencia evita duplicados`() {
        val tecnico = token(); val jefe = token("jefe1")
        fun crear() = mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer $tecnico").header("Idempotency-Key", "prueba-crud-unica").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response
        val creada = crear(); assertEquals(201, creada.status)
        val id = mapper.readTree(creada.contentAsString)["id"].asLong()
        assertEquals(id, mapper.readTree(crear().contentAsString)["id"].asLong())
        assertEquals(200, mvc.perform(put("/api/v1/labores/$id").header("Authorization", "Bearer $tecnico").contentType(MediaType.APPLICATION_JSON).content(labor(cantidad = 3.0))).andReturn().response.status)
        assertEquals(3.0, jdbc.queryForObject("SELECT cantidad FROM labor WHERE id=?", Double::class.java, id))
        val borrada = mvc.perform(delete("/api/v1/labores/$id").header("Authorization", "Bearer $jefe")).andReturn().response
        assertEquals(204, borrada.status); assertEquals("", borrada.contentAsString)
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM labor WHERE id=?", Int::class.java, id))
    }
    @Test fun `RN02 restringe autor y plazo sin comparar roles`() {
        val t = token(); val jefe = token("jefe1")
        val r = mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer $jefe").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response
        val id = mapper.readTree(r.contentAsString)["id"].asLong()
        assertEquals(403, mvc.perform(put("/api/v1/labores/$id").header("Authorization", "Bearer $t").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response.status)
        jdbc.update("UPDATE labor SET creada_en=DATEADD('HOUR',-25,CURRENT_TIMESTAMP),responsable_id=(SELECT id FROM usuario WHERE usuario='tecnico1') WHERE id=?", id)
        assertEquals(403, mvc.perform(put("/api/v1/labores/$id").header("Authorization", "Bearer $t").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response.status)
        assertEquals(200, mvc.perform(put("/api/v1/labores/$id").header("Authorization", "Bearer $jefe").contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response.status)
    }
    @Test fun `cierre con cosecha persiste y bloquea altas y correcciones`() {
        val jefe = token("jefe1")
        val creada = mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer $jefe").contentType(MediaType.APPLICATION_JSON).content(labor(3, "COSECHA"))).andReturn().response
        val id = mapper.readTree(creada.contentAsString)["id"].asLong()
        assertEquals(200, mvc.perform(post("/api/v1/lotes/3/cerrar-campania").header("Authorization", "Bearer $jefe")).andReturn().response.status)
        assertEquals(true, jdbc.queryForObject("SELECT campania_cerrada FROM lote WHERE id=3", Boolean::class.java))
        assertEquals(409, mvc.perform(put("/api/v1/labores/$id").header("Authorization", "Bearer $jefe").contentType(MediaType.APPLICATION_JSON).content(labor(3))).andReturn().response.status)
    }
    @Test fun `recurso ausente y paginacion invalida no producen 500`() {
        val t = token()
        assertEquals(404, mvc.perform(get("/api/v1/lotes/99999").header("Authorization", "Bearer $t")).andReturn().response.status)
        assertEquals(400, mvc.perform(get("/api/v1/lotes?pagina=-1&tamanio=0").header("Authorization", "Bearer $t")).andReturn().response.status)
    }
    @Test fun `fecha de registro se publica como instante UTC inequívoco`() {
        val r = mvc.perform(post("/api/v1/labores").header("Authorization", "Bearer ${token()}")
            .contentType(MediaType.APPLICATION_JSON).content(labor())).andReturn().response
        assertEquals(201, r.status)
        val creadaEn = mapper.readTree(r.contentAsString)["creadaEn"].asText()
        assertTrue(creadaEn.endsWith("Z"))
        assertTrue(java.time.Duration.between(Instant.parse(creadaEn), Instant.now()).abs().seconds < 5)
    }
}
