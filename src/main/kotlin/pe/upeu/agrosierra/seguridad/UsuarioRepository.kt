package pe.upeu.agrosierra.seguridad

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class UsuarioRepository(private val jdbc: JdbcTemplate) {
    fun porNombre(nombre: String): Usuario? = buscar("usuario", nombre)
    fun porId(id: Long): Usuario? = buscar("id", id)
    private fun buscar(campo: String, valor: Any): Usuario? {
        val filas = jdbc.query("SELECT * FROM usuario WHERE $campo = ?", { rs, _ ->
            Usuario(rs.getLong("id"), rs.getString("usuario"), rs.getString("nombre"),
                rs.getString("contrasena"), rs.getString("rol"), emptySet())
        }, valor)
        val u = filas.firstOrNull() ?: return null
        val permisos = jdbc.queryForList("SELECT privilegio FROM rol_privilegio WHERE rol = ?", String::class.java, u.rol)
        return u.copy(privilegios = permisos.toSet())
    }
}
