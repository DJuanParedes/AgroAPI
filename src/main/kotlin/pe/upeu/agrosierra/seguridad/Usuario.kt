package pe.upeu.agrosierra.seguridad

data class Usuario(
    val id: Long, val usuario: String, val nombre: String,
    val contrasena: String, val rol: String, val privilegios: Set<String>
)
