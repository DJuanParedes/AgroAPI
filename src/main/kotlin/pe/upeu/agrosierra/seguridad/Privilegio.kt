package pe.upeu.agrosierra.seguridad

enum class Privilegio {
    LOTE_LEER, LABOR_LEER, LABOR_CREAR, LABOR_EDITAR, LABOR_ELIMINAR,
    CAMPANIA_CERRAR, USUARIO_GESTIONAR,
    // Capacidad adicional: evita comprobar nombres de roles al aplicar RN-02.
    LABOR_EDITAR_TODAS
}
