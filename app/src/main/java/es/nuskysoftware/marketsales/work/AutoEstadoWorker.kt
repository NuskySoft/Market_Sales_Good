package es.nuskysoftware.marketsales.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager

/**
 * Worker que aplica los autoestados programados por AutoEstadoScheduler.
 * - Respeta tu lógica de 00:00 y 05:00 (la decide el propio repositorio).
 * - No muestra UI ni hace nada más.
 */
class AutoEstadoWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object { private const val TAG = "AutoEstadoWorker" }

    override suspend fun doWork(): Result {
        return try {
            val uid = ConfigurationManager.getCurrentUserId()
            if (uid.isNullOrBlank() || uid == "usuario_default") {
                Log.d(TAG, "Sin usuario válido; no se aplican autoestados")
                Result.success()
            } else {
                MercadilloRepository(applicationContext).actualizarEstadosAutomaticos(uid)
                Log.d(TAG, "Autoestados aplicados para $uid")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error aplicando autoestados", e)
            Result.retry()
        }
    }
}



//package es.nuskysoftware.marketsales.work
//
//import android.content.Context
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//
//class AutoEstadoWorker(
//    context: Context,
//    params: WorkerParameters
//) : CoroutineWorker(context, params) {
//    override suspend fun doWork(): Result {
//        return try {
//            val uid = ConfigurationManager.getCurrentUserId()
//            if (!uid.isNullOrBlank()) {
//                MercadilloRepository(applicationContext).actualizarEstadosAutomaticos(uid)
//            }
//            Result.success()
//        } catch (_: Exception) {
//            Result.retry()
//        }
//    }
//}
