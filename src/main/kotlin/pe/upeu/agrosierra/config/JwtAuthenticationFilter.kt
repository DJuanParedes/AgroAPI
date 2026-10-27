package pe.upeu.agrosierra.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import pe.upeu.agrosierra.seguridad.UsuarioRepository
import pe.upeu.agrosierra.web.dto.ErrorRespuesta

@Component
class JwtAuthenticationFilter(private val jwt: JwtService, private val usuarios: UsuarioRepository,
    private val mapper: ObjectMapper) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest) = request.servletPath == "/api/v1/auth/login"
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val header = req.getHeader("Authorization")
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) throw IllegalArgumentException("Esquema inválido")
                val u = usuarios.porId(jwt.usuarioId(header.removePrefix("Bearer ")))
                    ?: throw IllegalArgumentException("Usuario inexistente")
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                    u, null, u.privilegios.map(::SimpleGrantedAuthority))
            } catch (e: Exception) {
                if (e !is JwtException && e !is IllegalArgumentException) throw e
                SecurityContextHolder.clearContext()
                res.status = 401; res.contentType = "application/json"; res.characterEncoding = "UTF-8"
                mapper.writeValue(res.writer, ErrorRespuesta(status = 401, error = "Unauthorized",
                    message = "Sesión inválida o vencida", path = req.requestURI))
                return
            }
        }
        chain.doFilter(req, res)
    }
}
