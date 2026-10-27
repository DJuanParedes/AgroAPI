package pe.upeu.agrosierra.web

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import pe.upeu.agrosierra.seguridad.Usuario
import pe.upeu.agrosierra.servicio.AgroService
import pe.upeu.agrosierra.web.dto.LaborRequest
import java.net.URI

@RestController
@RequestMapping("/api/v1")
class AgroController(private val servicio: AgroService) {
    @GetMapping("/lotes") @PreAuthorize("hasAuthority('LOTE_LEER')")
    fun lotes(@RequestParam(defaultValue = "0") pagina: Int, @RequestParam(defaultValue = "20") tamanio: Int,
        @RequestParam(required = false) valle: String?, @RequestParam(required = false) campaniaCerrada: Boolean?) = servicio.lotes(valle, campaniaCerrada, pagina, tamanio)
    @GetMapping("/lotes/{id}") @PreAuthorize("hasAuthority('LOTE_LEER')")
    fun lote(@PathVariable id: Long) = servicio.lote(id)
    @GetMapping("/lotes/{id}/labores") @PreAuthorize("hasAuthority('LABOR_LEER')")
    fun labores(@PathVariable id: Long, @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int) = servicio.labores(id, pagina, tamanio)
    @PostMapping("/labores") @PreAuthorize("hasAuthority('LABOR_CREAR')")
    fun crear(@RequestBody req: LaborRequest, @AuthenticationPrincipal usuario: Usuario,
        @RequestHeader(name = "Idempotency-Key", required = false) clave: String?) = servicio.crear(req, usuario, clave).let {
        ResponseEntity.created(URI.create("/api/v1/labores/${it.id}")).body(it)
    }
    @PutMapping("/labores/{id}") @PreAuthorize("hasAuthority('LABOR_EDITAR')")
    fun editar(@PathVariable id: Long, @RequestBody req: LaborRequest, @AuthenticationPrincipal usuario: Usuario) = servicio.editar(id, req, usuario)
    @DeleteMapping("/labores/{id}") @PreAuthorize("hasAuthority('LABOR_ELIMINAR')")
    fun eliminar(@PathVariable id: Long): ResponseEntity<Void> { servicio.eliminar(id); return ResponseEntity.noContent().build() }
    @PostMapping("/lotes/{id}/cerrar-campania") @PreAuthorize("hasAuthority('CAMPANIA_CERRAR')")
    fun cerrar(@PathVariable id: Long) = servicio.cerrar(id)
}
