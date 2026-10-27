package pe.upeu.agrosierra.web.dto

data class LaborRequest(val loteId: Long = 0, val tipo: String = "", val fecha: String = "",
    val cantidad: Double = 0.0, val observacion: String = "")
