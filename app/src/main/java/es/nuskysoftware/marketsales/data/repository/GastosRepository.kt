// app/src/main/java/es/nuskysoftware/marketsales/data/repository/GastosRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    private val db = AppDatabase.getDatabase(context)
    private val gastosDao: LineasGastosDao = db.lineasGastosDao()
    private val mercadilloDao: MercadilloDao = db.mercadilloDao()
    private val fs = FirebaseFirestore.getInstance()
    private val connectivity = ConnectivityObserver(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            connectivity.isConnected.collect { online ->
                if (online) {
                    runCatching { pushPendientes() }
                        .onFailure { e -> Log.w(TAG, "Reintento de sync fallido", e) }
                }
            }
        }
    }

    fun observarGastos(idMercadillo: String) =
        gastosDao.observarGastosPorMercadillo(idMercadillo)

    suspend fun guardarGasto(
        idMercadillo: String,
        descripcion: String,
        importe: Double,
        formaPago: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
            val now = System.currentTimeMillis()

            val max = gastosDao.obtenerMaxNumeroLineaPorMercadillo(idMercadillo)
            val base = max?.toIntOrNull() ?: 0
            val next = String.format("%04d", base + 1)

            val linea = LineaGastoEntity(
                idMercadillo = idMercadillo,
                idUsuario = userId,
                numeroLinea = next,
                descripcion = descripcion,
                importe = importe,
                fechaHora = now,
                formaPago = formaPago,
                lastModified = now,
                sincronizadoFirebase = false
            )

            db.withTransaction {
                gastosDao.insertarLinea(linea)
                val totalG = gastosDao.getTotalGastosMercadillo(idMercadillo)
                val m = mercadilloDao.getMercadilloById(idMercadillo) ?: return@withTransaction
                mercadilloDao.actualizarTotales(
                    id = idMercadillo,
                    totalVentas = m.totalVentas,
                    totalGastos = totalG,
                    arqueoMercadillo = m.arqueoMercadillo ?: 0.0,
                    timestamp = System.currentTimeMillis()
                )
            }

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

    private suspend fun pushUno(l: LineaGastoEntity) {
        val docId = "${l.idMercadillo}-${l.numeroLinea}"
        val data = mapOf(
            "idMercadillo" to l.idMercadillo,
            "idUsuario" to l.idUsuario,
            "numeroLinea" to l.numeroLinea,
            "descripcion" to l.descripcion,
            "importe" to l.importe,
            "fechaHora" to l.fechaHora,
            "formaPago" to l.formaPago,
            "activo" to l.activo,
            "version" to l.version,
            "lastModified" to l.lastModified,
            "fechaSync" to System.currentTimeMillis()
        )
        fs.collection("LineasGastos").document(docId).set(data, SetOptions.merge()).await()
        gastosDao.marcarSincronizado(l.idMercadillo, l.numeroLinea, System.currentTimeMillis())
    }

    private suspend fun pushPendientes() {
        val userId = ConfigurationManager.getCurrentUserId() ?: return
        val pendientes = gastosDao.getPendientesSync(userId)
        pendientes.forEach { runCatching { pushUno(it) } }
    }
}


//package es.nuskysoftware.marketsales.data.repository
//
//import android.content.Context
//import android.util.Log
//import androidx.room.withTransaction
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.SetOptions
//import es.nuskysoftware.marketsales.data.local.dao.LineasGastosDao
//import es.nuskysoftware.marketsales.data.local.dao.MercadilloDao
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.ConnectivityObserver
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//
//class GastosRepository(context: Context) {
//
//    private val db = AppDatabase.getDatabase(context)
//    private val gastosDao: LineasGastosDao = db.lineasGastosDao()
//    private val mercadilloDao: MercadilloDao = db.mercadilloDao()
//    private val fs = FirebaseFirestore.getInstance()
//    private val connectivity = ConnectivityObserver(context)
//    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//    companion object { private const val TAG = "GastosRepository" }
//
//    init {
//        // Reintento best-effort cuando vuelve la conexión (subida de todo lo local)
//        scope.launch {
//            connectivity.isConnected.collect { online ->
//                if (online) {
//                    runCatching { pushPendientes() }
//                        .onFailure { e -> Log.w(TAG, "Reintento de sync fallido", e) }
//                }
//            }
//        }
//    }
//
//    fun observarGastos(idMercadillo: String): Flow<List<LineaGastoEntity>> =
//        gastosDao.observarGastosPorMercadillo(idMercadillo)
//
//    suspend fun guardarGasto(
//        idMercadillo: String,
//        descripcion: String,
//        importe: Double
//    ): Result<Unit> = withContext(Dispatchers.IO) {
//        try {
//            val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
//            val now = System.currentTimeMillis()
//
//            val max = gastosDao.obtenerMaxNumeroLineaPorMercadillo(idMercadillo)
//            val base = max?.toIntOrNull() ?: 0
//            val next = String.format("%04d", base + 1)
//
//            val linea = LineaGastoEntity(
//                idMercadillo = idMercadillo,
//                idUsuario = userId,
//                numeroLinea = next,
//                descripcion = descripcion,
//                importe = importe,
//                fechaHora = now
//            )
//
//            db.withTransaction {
//                gastosDao.insertarLinea(linea)
//                val totalG = gastosDao.getTotalGastosMercadillo(idMercadillo)
//                val m = mercadilloDao.getMercadilloById(idMercadillo) ?: return@withTransaction
//                mercadilloDao.actualizarTotales(
//                    id = idMercadillo,
//                    totalVentas = m.totalVentas,
//                    totalGastos = totalG,
//                    arqueoMercadillo = m.arqueoMercadillo ?: 0.0,
//                    timestamp = System.currentTimeMillis()
//                )
//            }
//
//
//            // Subida best-effort
//            if (connectivity.isConnected.first()) {
//                scope.launch {
//                    runCatching { pushUno(linea) }
//                        .onFailure { e -> Log.w(TAG, "Upload gasto falló (se reintentará)", e) }
//                }
//            }
//
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    private suspend fun pushPendientes() {
//        // Sube todas las líneas locales (idempotente por docId)
//        val userId = ConfigurationManager.getCurrentUserId() ?: return
//        val mercs = mercadilloDao.getMercadillosByUser(userId).first()
//        for (m in mercs) {
//            val lineas = gastosDao.observarGastosPorMercadillo(m.idMercadillo).first()
//            lineas.forEach { runCatching { pushUno(it) } }
//        }
//    }
//
//    private suspend fun pushUno(l: LineaGastoEntity) {
//        val docId = "${l.idMercadillo}-${l.numeroLinea}"
//        val data = mapOf(
//            "idMercadillo" to l.idMercadillo,
//            "idUsuario" to l.idUsuario,
//            "numeroLinea" to l.numeroLinea,
//            "descripcion" to l.descripcion,
//            "importe" to l.importe,
//            "fechaHora" to l.fechaHora
//        )
//        fs.collection("LineasGastos").document(docId).set(data, SetOptions.merge()).await()
//    }
//}
//
