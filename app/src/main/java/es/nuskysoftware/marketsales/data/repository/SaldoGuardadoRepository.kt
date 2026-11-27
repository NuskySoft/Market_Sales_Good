package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Gestiona saldos disponibles, asignación, consumo y retirada sin eliminar los demás saldos del propietario.
import android.content.Context
import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import es.nuskysoftware.marketsales.utils.*
import es.nuskysoftware.marketsales.work.BusinessSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
class SaldoGuardadoRepository(context: Context) {
    private val app = context.applicationContext
    private val db = AppDatabase.getDatabase(app)
    private val dao = db.saldoGuardadoDao()
    // Expone un flujo de saldos disponibles que cambia con el propietario activo.
    fun pendientes(): Flow<List<SaldoGuardadoEntity>> = ConfigurationManager.usuarioLogueado.flatMapLatest { uid ->
        dao.pendientes(uid ?: AppModePolicy.DEMO_USER_ID)
    }
    // Devuelve el primer saldo disponible, o null si no hay ninguno.
    suspend fun getUltimoNoConsumido(): SaldoGuardadoEntity? = pendientes().first().firstOrNull()
    // Compatibility name: now appends instead of deleting any previous balance.
    // Guarda un saldo adicional; el nombre histórico no significa sustituir otros saldos.
    suspend fun reemplazarGuardado(item: SaldoGuardadoEntity) = withContext(Dispatchers.IO) {
        DemoAccess.requireRegistered("demo_sin_asignar_saldo")
        RecoveryGate(app).beforeWrite(item.idUsuario)
        db.withTransaction {
            DemoAccess.requireOwner(item.idUsuario)
            dao.upsert(item.copy(saldoInicialGuardado = Money.round(item.saldoInicialGuardado), sincronizadoFirebase = false))
        }
        BusinessSyncScheduler.request(app)
    }
    // Asigna el saldo identificado al mercadillo de destino y actualiza su disponibilidad.
    suspend fun asignar(id: String, destino: String) = withContext(Dispatchers.IO) {
        DemoAccess.requireRegistered("demo_sin_asignar_saldo")
        val uid = ConfigurationManager.getCurrentUserId() ?: error("No owner")
        db.withTransaction {
            DemoAccess.requireOwner(uid)
            val balance = dao.byId(id, uid) ?: error("Balance unavailable")
            check(!balance.consumido && !balance.eliminado)
            val market = db.mercadilloDao().getMercadilloById(destino) ?: error("Market unavailable")
            check(market.userId == uid && market.estado in 1..3)
            db.mercadilloDao().asignarSaldoInicial(destino, balance.saldoInicialGuardado)
            dao.upsert(balance.copy(consumido = true, idMercadilloDestino = destino, version = balance.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
        }
        BusinessSyncScheduler.request(app)
    }
    // Retira el saldo de la lista disponible por consumo.
    suspend fun marcarConsumido(id: String) = change(id, false)
    // Marca la retirada del saldo solicitado.
    suspend fun borrarGuardado(id: String) = change(id, true)
    // Centraliza el cambio de disponibilidad y seguimiento de la modificación local.
    private suspend fun change(id: String, delete: Boolean) = withContext(Dispatchers.IO) {
        DemoAccess.requireRegistered("demo_sin_asignar_saldo")
        val uid = ConfigurationManager.getCurrentUserId() ?: error("No owner")
        db.withTransaction {
            DemoAccess.requireOwner(uid)
            val old = dao.byId(id, uid) ?: error("Balance unavailable")
            check(!old.consumido && !old.eliminado)
            dao.upsert(old.copy(consumido = !delete, eliminado = delete, version = old.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
        }
        BusinessSyncScheduler.request(app)
    }
}
