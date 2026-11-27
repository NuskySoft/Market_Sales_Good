package es.nuskysoftware.marketsales.data.repository

import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

internal object AutoEstadoPolicy {
    private val zone = ZoneId.of("Europe/Madrid")
    private val format = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun next(estado: Int, fecha: String, now: Instant, tieneVentas: Boolean): Int {
        if (estado !in 1..3) return estado
        val date = try { LocalDate.parse(fecha, format) } catch (_: java.time.DateTimeException) { return estado }
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atTime(5, 0).atZone(zone).toInstant()
        return when {
            !now.isBefore(end) -> if (tieneVentas) 4 else 7
            !now.isBefore(start) && estado in 1..2 -> 3
            else -> estado
        }
    }
}

internal class AutoEstadoProcessor(private val db: AppDatabase) {
    suspend fun apply(uid: String, now: Instant = Instant.now()): Int = db.withTransaction {
        val dao = db.mercadilloDao()
        var changed = 0
        for (market in dao.pendientesAutoestado(uid)) {
            val next = AutoEstadoPolicy.next(market.estado, market.fecha, now,
                db.lineasVentaDao().contarLineasPorMercadillo(market.idMercadillo, uid) > 0)
            if (next == market.estado) continue
            when (next) {
                3 -> dao.actualizarEstado(market.idMercadillo, next, now.toEpochMilli())
                4 -> dao.marcarPendienteArqueo(market.idMercadillo, now.toEpochMilli())
                7 -> dao.cancelarMercadillo(market.idMercadillo, now.toEpochMilli())
            }
            changed++
        }
        changed
    }
}
