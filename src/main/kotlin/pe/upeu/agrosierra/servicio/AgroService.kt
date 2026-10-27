package pe.upeu.agrosierra.servicio

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.config.FalloApi
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.repositorio.AgroRepository
import pe.upeu.agrosierra.seguridad.Usuario
import pe.upeu.agrosierra.web.dto.*
import java.time.LocalDate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeParseException

@Service
class AgroService(private val repo: AgroRepository) {
    fun lote(id: Long) = repo.lote(id) ?: throw FalloApi(HttpStatus.NOT_FOUND, "El lote no existe")
    fun lotes(valle: String?, estado: Boolean?, pagina: Int, tamanio: Int): Pagina<Lote> {
        validarPagina(pagina, tamanio)
        return paginar(repo.lotes(valle?.takeIf { it.isNotBlank() }, estado, pagina, tamanio), pagina, tamanio)
    }
    fun labores(id: Long, pagina: Int, tamanio: Int): Pagina<Labor> {
        lote(id); validarPagina(pagina, tamanio)
        return paginar(repo.labores(id, pagina, tamanio), pagina, tamanio)
    }
    @Transactional
    fun crear(req: LaborRequest, usuario: Usuario, clave: String?): Labor {
        if (clave != null && (clave.isBlank() || clave.length > 100)) throw FalloApi(HttpStatus.BAD_REQUEST, "Idempotency-Key inválida")
        val lote = repo.lote(req.loteId, true) ?: throw FalloApi(HttpStatus.NOT_FOUND, "El lote no existe")
        if (clave != null) repo.operacion(usuario.id, clave)?.let { return repo.labor(it) ?: throw FalloApi(HttpStatus.CONFLICT, "La operación ya fue eliminada") }
        abierto(lote)
        val (tipo, fecha) = validar(req, lote)
        val labor = repo.crear(lote.id, tipo, fecha, usuario.id, req.cantidad, req.observacion.trim())
        if (clave != null) repo.guardarOperacion(usuario.id, clave, labor.id)
        return labor
    }
    @Transactional
    fun editar(id: Long, req: LaborRequest, usuario: Usuario): Labor {
        val actual = repo.labor(id) ?: throw FalloApi(HttpStatus.NOT_FOUND, "La labor no existe")
        val lote = repo.lote(actual.loteId, true)!!
        abierto(lote)
        if (req.loteId != actual.loteId) throw FalloApi(HttpStatus.BAD_REQUEST, "No se puede trasladar una labor", mapOf("loteId" to "Debe conservar el lote original"))
        if ("LABOR_EDITAR_TODAS" !in usuario.privilegios &&
            (actual.responsableId != usuario.id || Instant.now().isAfter(actual.creadaEn.plus(24, ChronoUnit.HOURS)))) {
            throw FalloApi(HttpStatus.FORBIDDEN, "Solo puede corregir labores propias dentro de las primeras 24 horas")
        }
        val (tipo, fecha) = validar(req, lote)
        return repo.editar(id, tipo, fecha, req.cantidad, req.observacion.trim())
    }
    @Transactional
    fun eliminar(id: Long) {
        val actual = repo.labor(id) ?: throw FalloApi(HttpStatus.NOT_FOUND, "La labor no existe")
        repo.lote(actual.loteId, true)
        repo.eliminar(id)
    }
    @Transactional
    fun cerrar(id: Long): Lote {
        repo.lote(id, true) ?: throw FalloApi(HttpStatus.NOT_FOUND, "El lote no existe")
        if (!repo.tieneCosecha(id)) throw FalloApi(HttpStatus.CONFLICT, "Debe registrar al menos una cosecha antes de cerrar la campaña")
        repo.cerrar(id)
        return lote(id)
    }
    private fun abierto(lote: Lote) {
        if (lote.campaniaCerrada) throw FalloApi(HttpStatus.CONFLICT, "La campaña del lote está cerrada")
    }
    private fun validar(req: LaborRequest, lote: Lote): Pair<TipoLabor, LocalDate> {
        val errores = linkedMapOf<String, String>()
        val tipo = TipoLabor.entries.find { it.name == req.tipo }
        if (tipo == null) errores["tipo"] = "Seleccione RIEGO, FUMIGACION o COSECHA"
        val fecha = try { LocalDate.parse(req.fecha) } catch (_: DateTimeParseException) { null }
        if (fecha == null) errores["fecha"] = "Use una fecha válida con formato AAAA-MM-DD"
        if (!req.cantidad.isFinite() || req.cantidad <= 0) errores["cantidad"] = "La cantidad debe ser mayor que cero"
        if (tipo == TipoLabor.FUMIGACION && req.cantidad > lote.dosisMaximaPorHectarea * lote.hectareas)
            errores["cantidad"] = "La dosis máxima para este lote es ${lote.dosisMaximaPorHectarea * lote.hectareas}"
        if (req.observacion.length > 1000) errores["observacion"] = "Máximo 1000 caracteres"
        if (errores.isNotEmpty()) throw FalloApi(HttpStatus.BAD_REQUEST, "Datos inválidos", errores)
        return tipo!! to fecha!!
    }
    private fun validarPagina(p: Int, t: Int) {
        if (p < 0 || t !in 1..100) throw FalloApi(HttpStatus.BAD_REQUEST, "Paginación inválida", mapOf("pagina" to "Desde cero", "tamanio" to "Entre 1 y 100"))
    }
    private fun <T> paginar(datos: Pair<List<T>, Long>, p: Int, t: Int): Pagina<T> {
        val paginas = ((datos.second + t - 1) / t).toInt()
        return Pagina(datos.first, p, t, datos.second, paginas, p >= paginas - 1)
    }
}
