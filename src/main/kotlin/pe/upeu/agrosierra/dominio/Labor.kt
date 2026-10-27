package pe.upeu.agrosierra.dominio

import java.time.LocalDate
import java.time.Instant

enum class TipoLabor { RIEGO, FUMIGACION, COSECHA }
data class Labor(val id: Long, val loteId: Long, val tipo: TipoLabor, val fecha: LocalDate,
    val responsable: String, val cantidad: Double, val observacion: String,
    val responsableId: Long, val creadaEn: Instant)
