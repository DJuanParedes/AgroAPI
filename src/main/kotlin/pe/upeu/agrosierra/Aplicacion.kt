package pe.upeu.agrosierra

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class Aplicacion

fun main(args: Array<String>) { runApplication<Aplicacion>(*args) }
