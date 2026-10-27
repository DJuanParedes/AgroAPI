package pe.upeu.agrosierra.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.seguridad.Privilegio
import java.time.LocalDate

@Component
class DatosSemilla(private val jdbc: JdbcTemplate, private val encoder: BCryptPasswordEncoder) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        Privilegio.entries.forEach { jdbc.update("MERGE INTO privilegio(nombre) KEY(nombre) VALUES(?)", it.name) }
        val lectura = setOf("LOTE_LEER", "LABOR_LEER")
        val roles = mapOf("TECNICO" to lectura + setOf("LABOR_CREAR", "LABOR_EDITAR"),
            "JEFE_CAMPO" to lectura + setOf("LABOR_CREAR", "LABOR_EDITAR", "LABOR_EDITAR_TODAS", "LABOR_ELIMINAR", "CAMPANIA_CERRAR"),
            "ADMINISTRADOR" to lectura + "USUARIO_GESTIONAR")
        roles.forEach { (rol, permisos) ->
            jdbc.update("MERGE INTO rol(nombre) KEY(nombre) VALUES(?)", rol)
            permisos.forEach { jdbc.update("MERGE INTO rol_privilegio(rol,privilegio) KEY(rol,privilegio) VALUES(?,?)", rol, it) }
        }
        listOf(Triple("tecnico1", "Técnico de campo", "TECNICO"), Triple("jefe1", "Jefe de campo", "JEFE_CAMPO"), Triple("admin", "Administrador", "ADMINISTRADOR")).forEach { (usuario, nombre, rol) ->
            if (jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE usuario=?", Int::class.java, usuario) == 0)
                jdbc.update("INSERT INTO usuario(usuario,nombre,contrasena,rol) VALUES(?,?,?,?)", usuario, nombre, encoder.encode("Agro2026*"), rol)
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM lote", Int::class.java)!! > 0) return
        val responsable = jdbc.queryForObject("SELECT id FROM usuario WHERE usuario='tecnico1'", Long::class.java)!!
        for (id in 1L..8L) {
            jdbc.update("INSERT INTO lote(id,codigo,nombre,hectareas,cultivo,valle,campania_cerrada,dosis_maxima) VALUES(?,?,?,?,?,?,?,?)",
                id, "LOT-${id.toString().padStart(3, '0')}", "Lote $id", 2.0 + id, if (id % 2L == 0L) "Papa" else "Maíz",
                listOf("Mantaro", "Sagrado", "Colca")[(id.toInt() - 1) % 3], id >= 7, 3.0)
            val cantidad = if (id <= 4) 3 else 2
            for (n in 1..cantidad) {
                val tipo = if (id == 1L && n == 1) "COSECHA" else if (n == 2) "FUMIGACION" else "RIEGO"
                jdbc.update("INSERT INTO labor(lote_id,tipo,fecha,responsable_id,cantidad,observacion,creada_en) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                    id, tipo, LocalDate.now().minusDays(n.toLong()), responsable, 1.0 + n, "Labor semilla $n del lote $id")
            }
        }
    }
}
