package pe.upeu.agrosierra.config

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import pe.upeu.agrosierra.web.dto.ErrorRespuesta

class FalloApi(val estado: HttpStatus, override val message: String,
    val campos: Map<String, String> = emptyMap()) : RuntimeException(message)

@RestControllerAdvice
class ManejadorDeErrores {
    private val log = LoggerFactory.getLogger(javaClass)
    @ExceptionHandler(FalloApi::class)
    fun negocio(e: FalloApi, req: HttpServletRequest) = respuesta(e.estado, e.message, req, e.campos)
    @ExceptionHandler(AccessDeniedException::class)
    fun prohibido(e: Exception, req: HttpServletRequest) = respuesta(HttpStatus.FORBIDDEN, "No posee el privilegio requerido", req)
    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun formato(e: Exception, req: HttpServletRequest) = respuesta(HttpStatus.BAD_REQUEST, "Formato de petición inválido", req)
    @ExceptionHandler(Exception::class)
    fun inesperado(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorRespuesta> {
        log.error("Fallo no controlado en {}", req.requestURI, e)
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación", req)
    }
    private fun respuesta(estado: HttpStatus, mensaje: String, req: HttpServletRequest,
        campos: Map<String, String> = emptyMap()) = ResponseEntity.status(estado).body(
        ErrorRespuesta(status = estado.value(), error = estado.reasonPhrase,
            message = mensaje, path = req.requestURI, validationErrors = campos))
}
