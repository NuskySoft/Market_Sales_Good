// app/src/main/java/es/nuskysoftware/marketsales/data/repository/SaldoGuardadoRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SaldoGuardadoRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.saldoGuardadoDao()                    // ✅ ahora existe
    private val firestore = FirebaseFirestore.getInstance()
    private val connectivity = ConnectivityObserver(context)

    suspend fun getUltimoNoConsumido(): SaldoGuardadoEntity? = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
        dao.getUltimoNoConsumido(userId)
    }

    suspend fun reemplazarGuardado(item: SaldoGuardadoEntity) = withContext(Dispatchers.IO) {
        val userId = item.idUsuario
        val previo = dao.getUltimoNoConsumido(userId)
        if (previo != null) dao.deleteById(previo.idRegistro) // ✅ idRegistro existe en la Entity
        dao.upsert(item.copy(sincronizadoFirebase = false))
        sincronizar(item.idRegistro)
    }

    suspend fun marcarConsumido(id: String) = withContext(Dispatchers.IO) {
        dao.marcarConsumido(id)
        sincronizar(id)
    }

    private suspend fun sincronizar(id: String) {
        try {
            if (!connectivity.isConnected.first()) return
            val userId = ConfigurationManager.getCurrentUserId() ?: return
            val item = dao.getUltimoNoConsumido(userId) ?: return // si ya se consumió, no subimos nada
            val datos = mapOf(
                "idRegistro" to item.idRegistro,
                "idUsuario" to item.idUsuario,
                "idMercadilloOrigen" to item.idMercadilloOrigen,
                "fechaMercadillo" to item.fechaMercadillo,
                "lugarMercadillo" to item.lugarMercadillo,
                "organizadorMercadillo" to item.organizadorMercadillo,
                "horaInicioMercadillo" to item.horaInicioMercadillo,
                "saldoInicialGuardado" to item.saldoInicialGuardado,
                "consumido" to item.consumido,
                "version" to item.version,
                "lastModified" to item.lastModified,
                "sincronizadoFirebase" to true
            )
            firestore.collection("saldosGuardados")
                .document(item.idRegistro)
                .set(datos).await()
        } catch (_: Exception) { }
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/data/repository/SaldoGuardadoRepository.kt
//package es.nuskysoftware.marketsales.data.repository
//
//import android.content.Context
//import com.google.firebase.firestore.FirebaseFirestore
//import es.nuskysoftware.marketsales.data.local.dao.SaldoGuardadoDao
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.ConnectivityObserver
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//
//class SaldoGuardadoRepository(context: Context) {
//
//    private val db = AppDatabase.getDatabase(context)
//    private val dao: SaldoGuardadoDao = db.saldoGuardadoDao()
//    private val firestore = FirebaseFirestore.getInstance()
//    private val connectivity = ConnectivityObserver(context)
//
//    private val COLECCION = "saldosGuardados"
//
//    suspend fun getUltimoNoConsumido(): SaldoGuardadoEntity? = withContext(Dispatchers.IO) {
//        val userId = ConfigurationManager.getCurrentUserId() ?: return@withContext null
//        dao.getUltimoNoConsumido(userId)
//    }
//
//    /** Reemplaza cualquier saldo previo no consumido y guarda el nuevo. */
//    suspend fun reemplazarGuardado(nuevo: SaldoGuardadoEntity) = withContext(Dispatchers.IO) {
//        dao.borrarNoConsumidosDeUsuario(nuevo.idUsuario)
//        dao.upsert(nuevo.copy(
//            version = 1L,
//            lastModified = System.currentTimeMillis(),
//            sincronizadoFirebase = false
//        ))
//        syncUpsertIfOnline(nuevo.idRegistro)
//    }
//
//    suspend fun marcarConsumido(idRegistro: String) = withContext(Dispatchers.IO) {
//        dao.marcarConsumido(idRegistro)
//        syncUpsertIfOnline(idRegistro)
//    }
//
//    private suspend fun syncUpsertIfOnline(idRegistro: String) {
//        if (!connectivity.isConnected.first()) return
//        val row = dao.getById(idRegistro) ?: return
//        val data = mapOf(
//            "idRegistro" to row.idRegistro,
//            "idUsuario" to row.idUsuario,
//            "idMercadilloOrigen" to row.idMercadilloOrigen,
//            "fechaMercadillo" to row.fechaMercadillo,
//            "lugarMercadillo" to row.lugarMercadillo,
//            "organizadorMercadillo" to row.organizadorMercadillo,
//            "horaInicioMercadillo" to row.horaInicioMercadillo,
//            "saldoInicialGuardado" to row.saldoInicialGuardado,
//            "consumido" to row.consumido,
//            "version" to row.version,
//            "lastModified" to row.lastModified,
//            "sincronizadoFirebase" to true
//        )
//        firestore.collection(COLECCION).document(row.idRegistro).set(data).await()
//        dao.marcarSincronizado(row.idRegistro)
//    }
//}
