package es.nuskysoftware.marketsales.work

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.time.ZoneId
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit

/**
 * Programa el recálculo automático de estados a dos horas fijas:
 *  - 00:00 → pasar estados 1/2 → 3 (fin de día)
 *  - 05:00 → pasar estado 3 → 4 (inicio de arqueo)
 *
 * Además ofrece un disparo inmediato para ponerse al día (al abrir app, login,
 * salir de pantallas clave, etc.).
 */
object AutoEstadoScheduler {

    private const val WORK_00 = "autoEstado_diario_00"
    private const val WORK_05 = "autoEstado_diario_05"
    private const val WORK_ONCE = "autoEstado_once"

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleDaily(context: Context) {
        val wm = WorkManager.getInstance(context)

        wm.enqueueUniquePeriodicWork(
            WORK_00,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicAt(hour = 0, minute = 0)
        )

        wm.enqueueUniquePeriodicWork(
            WORK_05,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicAt(hour = 5, minute = 0)
        )
    }

    fun cancelDaily(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_00)
        wm.cancelUniqueWork(WORK_05)
    }

    /** Disparo inmediato para ponerse al día. */
    fun runOnceNow(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork(
            WORK_ONCE,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequest.from(AutoEstadoWorker::class.java)
        )
    }

    fun requestUpload(context: Context) {
        val request = OneTimeWorkRequest.Builder(AutoEstadoUploadWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "autoEstado_upload", ExistingWorkPolicy.APPEND_OR_REPLACE, request
        )
    }

    // ---------- helpers ----------

    @RequiresApi(Build.VERSION_CODES.O)
    private fun periodicAt(hour: Int, minute: Int): PeriodicWorkRequest {
        val now = ZonedDateTime.now(ZoneId.of("Europe/Madrid"))
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)
        val repeat = Duration.ofDays(1)

        return PeriodicWorkRequest.Builder(
            AutoEstadoWorker::class.java,
            repeat.toHours(), TimeUnit.HOURS
        )
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .build()
    }
}

