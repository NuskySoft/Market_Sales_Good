package es.nuskysoftware.marketsales.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object AutoEstadoScheduler {

    private val tz: TimeZone = TimeZone.getDefault() // hora del dispositivo

    private fun nextDelayTo(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance(tz)
        val target = Calendar.getInstance(tz).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    /** Lo llama la pantalla para programar los jobs diarios a las 00:00 y 05:00. */
    fun scheduleDaily(context: Context) {
        val wm = WorkManager.getInstance(context)

        val delay00 = nextDelayTo(0, 0)
        val delay05 = nextDelayTo(5, 0)

        val w00 = PeriodicWorkRequestBuilder<AutoEstadoWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay00, TimeUnit.MILLISECONDS)
            .addTag("autoestado-00")
            .build()

        val w05 = PeriodicWorkRequestBuilder<AutoEstadoWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay05, TimeUnit.MILLISECONDS)
            .addTag("autoestado-05")
            .build()

        wm.enqueueUniquePeriodicWork("autoestado-00", ExistingPeriodicWorkPolicy.UPDATE, w00)
        wm.enqueueUniquePeriodicWork("autoestado-05", ExistingPeriodicWorkPolicy.UPDATE, w05)
    }

    /** Disparo inmediato que tu UI invoca al abrir la app. */
    fun runOnceNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<AutoEstadoWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("auto_estado_run_once_now", ExistingWorkPolicy.REPLACE, req)
    }

    /** Compat si en alguna parte llamas a este nombre. */
    fun recalcNow(context: Context) = runOnceNow(context)
}



//package es.nuskysoftware.marketsales.work
//
//import android.content.Context
//import android.os.Build
//import androidx.annotation.RequiresApi
//import androidx.work.*
//import java.time.Duration
//import java.time.ZoneId
//import java.time.ZonedDateTime
//import java.util.concurrent.TimeUnit
//
///**
// * Programa dos trabajos DIARIOS según la hora LOCAL del dispositivo:
// *  - 00:00 → PROGRAMADO_* → EN_CURSO si corresponde
// *  - 05:00 → EN_CURSO → PENDIENTE_ARQUEO / CANCELADO
// * Y ofrece un disparo inmediato para cuando tú quieras (login, editar, arqueo, saldo, splash inicial, logout).
// */
//object AutoEstadoScheduler {
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun delayHasta(hour: Int, minute: Int, zone: ZoneId): Duration {
//        val ahora = ZonedDateTime.now(zone)
//        var proxima = ahora.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
//        if (!proxima.isAfter(ahora)) proxima = proxima.plusDays(1)
//        return Duration.between(ahora, proxima)
//    }
//
//    /** Llama a esto UNA VEZ al abrir la app para programar 00:00 y 05:00. */
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun scheduleDaily(context: Context) {
//        val zona = ZoneId.systemDefault() // ⬅️ hora del dispositivo (lo que pediste)
//        val wm = WorkManager.getInstance(context)
//
//        val d00 = delayHasta(0, 0, zona).toMillis()
//        val d05 = delayHasta(5, 0, zona).toMillis()
//
//        val req00 = PeriodicWorkRequestBuilder<AutoEstadoWorker>(24, TimeUnit.HOURS)
//            .setInitialDelay(d00, TimeUnit.MILLISECONDS)
//            .addTag("ESTADOS_00")
//            .build()
//
//        val req05 = PeriodicWorkRequestBuilder<AutoEstadoWorker>(24, TimeUnit.HOURS)
//            .setInitialDelay(d05, TimeUnit.MILLISECONDS)
//            .addTag("ESTADOS_05")
//            .build()
//
//        wm.enqueueUniquePeriodicWork("ESTADOS_00", ExistingPeriodicWorkPolicy.UPDATE, req00)
//        wm.enqueueUniquePeriodicWork("ESTADOS_05", ExistingPeriodicWorkPolicy.UPDATE, req05)
//    }
//
//    /** Disparo inmediato de recálculo. Úsalo tras crear/editar, arqueo, asignar saldo, login, import inicial, logout. */
//    fun recalcNow(context: Context) {
//        WorkManager.getInstance(context).enqueue(
//            OneTimeWorkRequestBuilder<AutoEstadoWorker>()
//                .addTag("ESTADOS_NOW")
//                .build()
//        )
//    }
//}
//
