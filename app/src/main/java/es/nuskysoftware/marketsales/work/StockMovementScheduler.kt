package es.nuskysoftware.marketsales.work

// Responsabilidad: Programa los reintentos de movimientos de stock locales y de su subida a Firebase.

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object StockMovementScheduler {
    fun start(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            "stock-pending-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(StockMovementWorker::class.java, 15, TimeUnit.MINUTES).build()
        )
        request(context)
    }

    fun request(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "stock-pending-local", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequest.Builder(StockMovementWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()
        )
        requestUpload(context)
    }

    // Encola un trabajo de subida condicionado por la disponibilidad de red.
    fun requestUpload(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "stock-pending-upload", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequest.Builder(StockUploadWorker::class.java)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()
        )
    }
}
