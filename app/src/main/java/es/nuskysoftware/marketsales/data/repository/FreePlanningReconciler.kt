package es.nuskysoftware.marketsales.data.repository

import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.utils.DemoAccess
import java.time.LocalDate
import java.time.ZoneId

/**
 * Adapta la planificación tras pasar a Free, sin perder historial.
 * Se ejecuta después de conciliar datos de negocio, con protección frente a cambios de cuenta.
 * La fecha de creación desconocida en registros anteriores a 2.6 impide elegir con seguridad:
 * en ese caso se conserva el grupo, en lugar de suponer un orden y retirar un mercadillo incorrecto.
 */
internal class FreePlanningReconciler(private val db: AppDatabase) {
    suspend fun apply(uid: String): Int = db.withTransaction {
        if (!DemoAccess.canSync(uid) || db.userDao().getUserById(uid)?.esPremium != false) return@withTransaction 0
        val dao = db.mercadilloDao()
        val today = LocalDate.now(ZoneId.of("Europe/Madrid")).toEpochDay()
        var changed = 0
        for (group in dao.planificadosFuturos(uid, today).groupBy { it.fechaEpochDay }.values) {
            if (group.size < 2 || group.any { it.creadoEn == null }) continue
            // Desempate estable si dos altas comparten exactamente la marca temporal.
            val ordered = group.sortedWith(compareBy({ it.creadoEn }, { it.idMercadillo }))
            for (market in ordered.drop(1)) {
                if (market.estado !in 1..2 || dao.tieneOperaciones(uid, market.idMercadillo) ||
                    market.totalVentas != 0.0 || market.totalGastos != 0.0 || market.saldoFinal != null ||
                    market.arqueoCaja != null || market.arqueoMercadillo != null) continue
                DemoAccess.requireOwner(uid)
                dao.updateMercadillo(market.copy(activo = false, version = market.version + 1,
                    lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
                changed++
            }
        }
        changed
    }
}
