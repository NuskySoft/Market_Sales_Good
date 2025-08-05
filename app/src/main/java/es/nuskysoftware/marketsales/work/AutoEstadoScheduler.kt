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
import java.time.LocalDateTime
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
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicAt(hour = 0, minute = 0)
        )

        wm.enqueueUniquePeriodicWork(
            WORK_05,
            ExistingPeriodicWorkPolicy.UPDATE,
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

    // ---------- helpers ----------

    @RequiresApi(Build.VERSION_CODES.O)
    private fun periodicAt(hour: Int, minute: Int): PeriodicWorkRequest {
        val now = LocalDateTime.now()
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



//package es.nuskysoftware.marketsales.work
//
//import android.content.Context
//import androidx.work.*
//import java.util.Calendar
//import java.util.TimeZone
//import java.util.concurrent.TimeUnit
//
//object AutoEstadoScheduler {
//
//    private val tz = TimeZone.getTimeZone("Europe/Madrid")
//
//    private fun nextDelayTo(hour: Int, minute: Int): Long {
//        val now = Calendar.getInstance(tz)
//        val target = Calendar.getInstance(tz).apply {
//            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
//            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
//            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
//        }
//        return target.timeInMillis - now.timeInMillis
//    }
//
//    fun scheduleDaily(context: Context) {
//        val wm = WorkManager.getInstance(context)
//
//        val w00 = PeriodicWorkRequestBuilder<AutoEstadoWorker>(24, TimeUnit.HOURS)
//            .setInitialDelay(nextDelayTo(0, 0), TimeUnit.MILLISECONDS)
//            .addTag("autoestado-00")
//            .build()
//
//        val w05 = PeriodicWorkRequestBuilder<AutoEstadoWorker>(24, TimeUnit.HOURS)
//            .setInitialDelay(nextDelayTo(5, 0), TimeUnit.MILLISECONDS)
//            .addTag("autoestado-05")
//            .build()
//
//        wm.enqueueUniquePeriodicWork("autoestado-00", ExistingPeriodicWorkPolicy.UPDATE, w00)
//        wm.enqueueUniquePeriodicWork("autoestado-05", ExistingPeriodicWorkPolicy.UPDATE, w05)
//    }
//
//    fun runOnceNow(context: Context) {
//        WorkManager.getInstance(context).enqueueUniqueWork(
//            "autoestado-on-open",
//            ExistingWorkPolicy.REPLACE,
//            OneTimeWorkRequestBuilder<AutoEstadoWorker>().build()
//        )
//    }
//}
