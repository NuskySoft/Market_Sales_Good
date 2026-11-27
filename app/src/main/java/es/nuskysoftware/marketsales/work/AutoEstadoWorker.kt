package es.nuskysoftware.marketsales.work

// Responsabilidad: Ejecuta el recálculo de estados y el envío pendiente como trabajos separados de WorkManager.

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.repository.AutoEstadoProcessor
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.DemoAccess
import es.nuskysoftware.marketsales.utils.AppModePolicy
import kotlinx.coroutines.CancellationException

class AutoEstadoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // Ejecuta el trabajo programado; el resultado determina si WorkManager debe reintentar.
    override suspend fun doWork(): Result = try {
        val uid = ConfigurationManager.getCurrentUserId()
        if (!uid.isNullOrBlank() && uid != AppModePolicy.DEMO_USER_ID) {
            AutoEstadoProcessor(AppDatabase.getDatabase(applicationContext)).apply(uid)
            AutoEstadoScheduler.requestUpload(applicationContext)
        }
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Log.w("AutoEstadoWorker", "Reintento local", failure)
        Result.retry()
    }
}

class AutoEstadoUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // Ejecuta el trabajo programado; el resultado determina si WorkManager debe reintentar.
    override suspend fun doWork(): Result {
        val uid = ConfigurationManager.getCurrentUserId() ?: return Result.success()
        if (!DemoAccess.canSync(uid)) return Result.success()
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            MercadilloRepository(applicationContext, db, observeNetwork = false).use { repo ->
                repo.sincronizarPendientesAutoestado(uid)
            }
            if (db.mercadilloDao().getMercadillosNoSincronizadosByUser(uid).isEmpty()) Result.success()
            else Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("AutoEstadoUploadWorker", "Reintento remoto", failure)
            Result.retry()
        }
    }
}
