// app/src/main/java/es/nuskysoftware/marketsales/data/repository/GastosRepository.kt
package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Registra gastos asociados a un mercadillo, actualiza sus totales y solicita sincronización después de guardar localmente.
import es.nuskysoftware.marketsales.utils.DemoAccess


import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.LineasGastosDao
import es.nuskysoftware.marketsales.data.local.dao.MercadilloDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GastosRepository(context: Context) {

    companion object {
        private const val TAG = "GastosRepository"
    }

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(context)
    private val gastosDao: LineasGastosDao = db.lineasGastosDao()
    private val mercadilloDao: MercadilloDao = db.mercadilloDao()
    private val fs = FirebaseFirestore.getInstance()
    private val connectivity = ConnectivityObserver(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    fun observarGastos(idMercadillo: String) =
        gastosDao.observarGastosPorMercadillo(idMercadillo)

    // Guarda el gasto y actualiza los totales del mercadillo dentro de la transacción local.
    suspend fun guardarGasto(
        idMercadillo: String,
        descripcion: String,
        importe: Double,
        formaPago: String,
        descontarDeCaja: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = ConfigurationManager.getCurrentUserId() ?: es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID
            RecoveryGate(appContext).beforeWrite(userId)
            DemoAccess.requireOwner(userId)
            val linea = db.withTransaction {
                val market = mercadilloDao.getMercadilloById(idMercadillo) ?: error("Market not found")
                DemoAccess.requireOwner(market.userId)
                // Una baja lógica de planificación también invalida una pantalla antigua de gastos.
                check(market.activo && market.estado !in 4..7) { "Market does not allow expenses" }
                val now = System.currentTimeMillis()
                val max = gastosDao.obtenerMaxNumeroLineaPorMercadillo(idMercadillo)
                val base = max?.substringBefore('-')?.toIntOrNull() ?: 0
                val next = String.format("%04d", base + 1) + "-" + java.util.UUID.randomUUID().toString()

                val linea = LineaGastoEntity(
                    idMercadillo = idMercadillo,
                    idUsuario = userId,
                    numeroLinea = next,
                    descripcion = descripcion,
                    importe = es.nuskysoftware.marketsales.utils.Money.round(importe),
                    fechaHora = now,
                    formaPago = formaPago,
                    descontarDeCaja = descontarDeCaja,
                    lastModified = now,
                    sincronizadoFirebase = false
                )

                gastosDao.insertarLinea(linea)
                val totalG = gastosDao.getTotalGastosMercadillo(idMercadillo)
                mercadilloDao.actualizarTotales(
                    id = idMercadillo,
                    totalVentas = market.totalVentas,
                    totalGastos = totalG,
                    arqueoMercadillo = market.arqueoMercadillo ?: 0.0,
                    timestamp = System.currentTimeMillis()
                )
                linea
            }

            es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
            if (connectivity.isConnected.first()) {
                scope.launch {
                    runCatching { pushUno(linea) }
                        .onFailure { e -> Log.w(TAG, "Upload gasto falló (se reintentará)", e) }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun guardarGastos(idMercadillo: String, lineas: List<Pair<String, Double>>, formaPago: String, descontarDeCaja: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                for ((descripcion, importe) in lineas) guardarGasto(idMercadillo, descripcion, importe, formaPago, descontarDeCaja).getOrThrow()
            }
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
    }

    private suspend fun pushUno(l: LineaGastoEntity) {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    private suspend fun pushPendientes() {
        val userId = ConfigurationManager.getCurrentUserId() ?: return
        val pendientes = gastosDao.getPendientesSync(userId)
        pendientes.forEach { runCatching { pushUno(it) } }
    }
}
