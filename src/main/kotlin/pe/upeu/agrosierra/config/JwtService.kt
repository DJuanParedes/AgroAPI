package pe.upeu.agrosierra.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.agrosierra.seguridad.Usuario
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.Date

@Service
class JwtService(@Value("\${agro.jwt-secret:}") secreto: String) {
    // En desarrollo la clave se genera una vez y persiste fuera de Git.
    // En despliegue se configura JWT_SECRET con al menos 32 bytes.
    private val key = Keys.hmacShaKeyFor(if (secreto.isNotBlank()) {
        require(secreto.toByteArray().size >= 32) { "JWT_SECRET requiere al menos 32 bytes" }
        secreto.toByteArray()
    } else {
        val ruta = Path.of("data", "jwt.key")
        Files.createDirectories(ruta.parent)
        if (!Files.exists(ruta)) {
            val bytes = ByteArray(64).also { SecureRandom().nextBytes(it) }
            Files.writeString(ruta, Base64.getEncoder().encodeToString(bytes))
        }
        Base64.getDecoder().decode(Files.readString(ruta))
    })
    fun emitir(usuario: Usuario): String = Jwts.builder()
        .subject(usuario.id.toString()).issuer("AgroAPI")
        .claim("privilegios", usuario.privilegios).claim("rol", usuario.rol)
        .issuedAt(Date.from(Instant.now())).expiration(Date.from(Instant.now().plusSeconds(3600)))
        .signWith(key).compact()

    fun usuarioId(token: String): Long = Jwts.parser().verifyWith(key).requireIssuer("AgroAPI")
        .build().parseSignedClaims(token).payload.subject.toLong()
}
