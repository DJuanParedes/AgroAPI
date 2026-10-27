package pe.upeu.agrosierra.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pe.upeu.agrosierra.web.dto.ErrorRespuesta

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean fun passwordEncoder() = BCryptPasswordEncoder()
    @Bean fun seguridad(http: HttpSecurity, filtro: JwtAuthenticationFilter, mapper: ObjectMapper): SecurityFilterChain {
        http.csrf { it.disable() }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.requestMatchers("/api/v1/auth/login").permitAll().anyRequest().authenticated() }
            .exceptionHandling { config ->
                config.authenticationEntryPoint { req, res, _ ->
                    res.status = 401; res.contentType = "application/json"; res.characterEncoding = "UTF-8"
                    mapper.writeValue(res.writer, ErrorRespuesta(status = 401, error = "Unauthorized", message = "Debe iniciar sesión", path = req.requestURI))
                }
                config.accessDeniedHandler { req, res, _ ->
                    res.status = 403; res.contentType = "application/json"; res.characterEncoding = "UTF-8"
                    mapper.writeValue(res.writer, ErrorRespuesta(status = 403, error = "Forbidden", message = "No posee el privilegio requerido", path = req.requestURI))
                }
            }.addFilterBefore(filtro, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
