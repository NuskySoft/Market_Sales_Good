package es.nuskysoftware.marketsales.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import es.nuskysoftware.marketsales.data.repository.SyncEngine
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.DemoAccess
import kotlinx.coroutines.CancellationException

object BusinessSyncScheduler {
    private fun constraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun start(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("business_sync_periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BusinessSyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints()).build())
        request(context)
    }
    fun request(context: Context) {
        runCatching { WorkManager.getInstance(context).enqueueUniqueWork("business_sync", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<BusinessSyncWorker>().setConstraints(constraints()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()) }
    }
}
class BusinessSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val uid = ConfigurationManager.getCurrentUserId()
        if (uid != null && DemoAccess.canSync(uid)) SyncEngine(applicationContext).sync(uid)
        Result.success()
    } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { Result.retry() }
}
