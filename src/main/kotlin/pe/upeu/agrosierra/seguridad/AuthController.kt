package pe.upeu.agrosierra.seguridad

import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.config.FalloApi
import pe.upeu.agrosierra.config.JwtService
import pe.upeu.agrosierra.web.dto.*

@RestController
@RequestMapping("/api/v1")
class AuthController(private val usuarios: UsuarioRepository, private val encoder: BCryptPasswordEncoder,
    private val jwt: JwtService) {
    private val hashFalso = encoder.encode("comparacion-de-tiempo")
    @PostMapping("/auth/login")
    fun login(@RequestBody req: LoginRequest): SesionRespuesta {
        val u = usuarios.porNombre(req.usuario)
        val coincide = encoder.matches(req.contrasena, u?.contrasena ?: hashFalso)
        if (u == null || !coincide) throw FalloApi(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos")
        return SesionRespuesta(jwt.emitir(u), u.usuario, u.nombre, u.rol, u.privilegios)
    }
    @GetMapping("/usuarios/me")
    fun actual(@AuthenticationPrincipal u: Usuario) = UsuarioRespuesta(u.id, u.usuario, u.nombre, u.rol, u.privilegios)
}
