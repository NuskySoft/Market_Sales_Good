package es.nuskysoftware.marketsales.work

// Responsabilidad: Ejecuta la recuperación de movimientos de existencias y su sincronización en trabajos independientes.

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.repository.ArticuloRepository
import es.nuskysoftware.marketsales.data.repository.StockMovementProcessor
import kotlinx.coroutines.CancellationException

/** Local retry must work without internet and even if the user has explicitly signed out. */
class StockMovementWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // Ejecuta el trabajo programado; el resultado determina si WorkManager debe reintentar.
    override suspend fun doWork(): Result = try {
        val finished = StockMovementProcessor(AppDatabase.getDatabase(applicationContext)).processPending()
        StockMovementScheduler.requestUpload(applicationContext)
        if (finished) Result.success() else Result.retry()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Log.w("StockMovementWorker", "Retrying local stock", failure)
        Result.retry()
    }
}

/** Network work is separate, so lack of internet never delays the local stock adjustment. */
class StockUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // Ejecuta el trabajo programado; el resultado determina si WorkManager debe reintentar.
    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            ArticuloRepository(applicationContext, db, observeNetwork = false).use { repo ->
                repo.sincronizarPendientesLocales(uid)
            }
            if (db.articuloDao().getArticulosNoSincronizadosByUser(uid).isEmpty()) Result.success() else Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("StockUploadWorker", "Retrying stock upload", failure)
            Result.retry()
        }
    }
}
