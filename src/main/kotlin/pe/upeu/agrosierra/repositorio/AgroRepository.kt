package pe.upeu.agrosierra.repositorio

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import pe.upeu.agrosierra.dominio.*
import java.sql.Statement
import java.time.LocalDate

@Repository
class AgroRepository(private val jdbc: JdbcTemplate) {
    private val loteMapper = RowMapper { rs, _ -> Lote(rs.getLong("id"), rs.getString("codigo"),
        rs.getString("nombre"), rs.getDouble("hectareas"), rs.getString("cultivo"), rs.getString("valle"),
        rs.getBoolean("campania_cerrada"), rs.getDouble("dosis_maxima")) }
    private val laborMapper = RowMapper { rs, _ -> Labor(rs.getLong("id"), rs.getLong("lote_id"),
        TipoLabor.valueOf(rs.getString("tipo")), rs.getDate("fecha").toLocalDate(), rs.getString("responsable"),
        rs.getDouble("cantidad"), rs.getString("observacion"), rs.getLong("responsable_id"),
        rs.getTimestamp("creada_en").toLocalDateTime()) }
    fun lote(id: Long, bloquear: Boolean = false): Lote? = jdbc.query(
        "SELECT * FROM lote WHERE id = ?" + if (bloquear) " FOR UPDATE" else "", loteMapper, id).firstOrNull()
    fun lotes(valle: String?, estado: Boolean?, pagina: Int, tamanio: Int): Pair<List<Lote>, Long> {
        val where = " WHERE (? IS NULL OR valle = ?) AND (? IS NULL OR campania_cerrada = ?)"
        val args = arrayOf<Any?>(valle, valle, estado, estado)
        val total = jdbc.queryForObject("SELECT COUNT(*) FROM lote$where", Long::class.java, *args)!!
        return jdbc.query("SELECT * FROM lote$where ORDER BY codigo LIMIT ? OFFSET ?", loteMapper,
            *args, tamanio, pagina.toLong() * tamanio) to total
    }
    fun labores(loteId: Long, pagina: Int, tamanio: Int): Pair<List<Labor>, Long> {
        val total = jdbc.queryForObject("SELECT COUNT(*) FROM labor WHERE lote_id = ?", Long::class.java, loteId)!!
        return jdbc.query("SELECT l.*, u.nombre AS responsable FROM labor l JOIN usuario u ON u.id=l.responsable_id WHERE lote_id=? ORDER BY fecha DESC, l.id DESC LIMIT ? OFFSET ?",
            laborMapper, loteId, tamanio, pagina.toLong() * tamanio) to total
    }
    fun labor(id: Long): Labor? = jdbc.query("SELECT l.*, u.nombre AS responsable FROM labor l JOIN usuario u ON u.id=l.responsable_id WHERE l.id=?", laborMapper, id).firstOrNull()
    fun crear(loteId: Long, tipo: TipoLabor, fecha: LocalDate, usuarioId: Long, cantidad: Double, observacion: String): Labor {
        val key = GeneratedKeyHolder()
        jdbc.update({ con -> con.prepareStatement("INSERT INTO labor(lote_id,tipo,fecha,responsable_id,cantidad,observacion,creada_en) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS).apply {
            setLong(1, loteId); setString(2, tipo.name); setObject(3, fecha); setLong(4, usuarioId)
            setDouble(5, cantidad); setString(6, observacion)
        } }, key)
        return labor(key.key!!.toLong())!!
    }
    fun editar(id: Long, tipo: TipoLabor, fecha: LocalDate, cantidad: Double, observacion: String): Labor {
        jdbc.update("UPDATE labor SET tipo=?,fecha=?,cantidad=?,observacion=? WHERE id=?", tipo.name, fecha, cantidad, observacion, id)
        return labor(id)!!
    }
    fun eliminar(id: Long) { jdbc.update("DELETE FROM operacion WHERE labor_id=?", id); jdbc.update("DELETE FROM labor WHERE id=?", id) }
    fun tieneCosecha(id: Long) = jdbc.queryForObject("SELECT COUNT(*) FROM labor WHERE lote_id=? AND tipo='COSECHA'", Long::class.java, id)!! > 0
    fun cerrar(id: Long) { jdbc.update("UPDATE lote SET campania_cerrada=TRUE WHERE id=?", id) }
    fun operacion(usuarioId: Long, clave: String) = jdbc.queryForList("SELECT labor_id FROM operacion WHERE usuario_id=? AND clave=?", Long::class.java, usuarioId, clave).firstOrNull()
    fun guardarOperacion(usuarioId: Long, clave: String, id: Long) { jdbc.update("INSERT INTO operacion(usuario_id,clave,labor_id) VALUES(?,?,?)", usuarioId, clave, id) }
}
