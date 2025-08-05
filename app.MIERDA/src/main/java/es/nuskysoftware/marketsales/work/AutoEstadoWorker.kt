package es.nuskysoftware.marketsales.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository

/**
 * Recalcula estados UNA ÚNICA VEZ cuando se ejecuta.
 * Usado por los disparos de 00:00, 05:00 y por los disparos manuales (login, arqueo, etc.).
 */
class AutoEstadoWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return try {
            if (!uid.isNullOrBlank()) {
                MercadilloRepository(applicationContext).actualizarEstadosAutomaticos(uid)
                Log.i("WORK", "[ESTADOS] Recalc OK para uid=$uid")
            } else {
                Log.w("WORK", "[ESTADOS] Sin uid (no recalculo)")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("WORK", "[ESTADOS] Error en recálculo: ${e.message}", e)
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
