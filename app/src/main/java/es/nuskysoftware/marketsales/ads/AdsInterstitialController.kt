package es.nuskysoftware.marketsales.ads

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.BuildConfig
//import es.nuskysoftware.marketsales.BuildConfig
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Interstitial unificado:
 * - Usa test unit en DEBUG y el id real en release.
 * - Respeta consentimiento (UMP) y estado Premium.
 * - Control de frecuencia (intervalo mínimo + tope diario).
 * - Precarga automática tras mostrar/fallar.
 *
 * Uso típico:
 *   AdsInterstitialController.preload(context)
 *   AdsInterstitialController.maybeShow(activity) { continuar() }
 */
object AdsInterstitialController {

    private const val TAG = "AdsInterstitial"

    // ======= POLÍTICA DE FRECUENCIA =======
    /** Si quieres que salga "siempre", pon 0L (no recomendado para publicación). */
    private const val MIN_INTERVAL_MS = 0L        // p.ej. 30_000L para 30s
    /** Tope diario. Sube/baja según tus pruebas (¡ojo con políticas!). */
    private const val MAX_PER_DAY = 1          // p.ej. 6 para producción

    // ======= PREFERENCIAS (conteo/fecha) =======
    private const val PREFS = "ads_prefs"
    private const val KEY_LAST_MS = "inter_last_ms"
    private const val KEY_DAY = "inter_day"
    private const val KEY_COUNT_DAY = "inter_count_day"

    @Volatile private var interstitial: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)
    @Volatile private var onAfterClose: (() -> Unit)? = null

    private fun adUnitId(context: Context): String =
        if (BuildConfig.DEBUG) {
            // Test ID oficial de Google para Interstitials
            "ca-app-pub-3940256099942544/1033173712"
        } else {
            context.getString(R.string.admob_interstitial_id)
        }

    /** Llama tras MobileAds.initialize() y cada vez que quieras tener uno preparado. */
    fun preload(context: Context) {
        if (isLoading.get() || interstitial != null) return
        if (!canServeAds()) return

        val appCtx = context.applicationContext
        val unitId = adUnitId(appCtx)

        isLoading.set(true)
        InterstitialAd.load(
            appCtx,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    isLoading.set(false)
                    attachCallbacks(appCtx, ad)
                    Log.d(TAG, "Interstitial cargado.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    isLoading.set(false)
                    Log.w(TAG, "Fallo al cargar interstitial: ${error.message}")
                }
            }
        )
    }

    /**
     * Muestra el interstitial si:
     *  - hay consentimiento UMP,
     *  - NO es premium,
     *  - respeta intervalo mínimo y tope diario,
     *  - y está precargado.
     * Si no cumple, ejecuta inmediatamente [onFinished].
     */
    fun maybeShow(activity: Activity, onFinished: () -> Unit = {}) {
        if (!canServeAds() || !isWithinPolicy(activity)) {
            preload(activity)
            onFinished()
            return
        }

        val ad = interstitial
        if (ad == null) {
            preload(activity)
            onFinished()
            return
        }

        // Marca uso y registra callback de cierre
        markShown(activity)
        onAfterClose = onFinished
        ad.show(activity)
    }

    // 💡 NUEVO: permitir “tirar” el interstitial precargado (cuando el usuario pasa a premium)
    fun dropPreloaded() {
        interstitial = null
        isLoading.set(false)
    }

    // ======= Internos =======

    private fun attachCallbacks(context: Context, ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial mostrado.")
                interstitial = null // invalidar referencia para forzar nueva precarga
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial cerrado.")
                val cb = onAfterClose
                onAfterClose = null
                preload(context)
                cb?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Error al mostrar interstitial: ${adError.message}")
                val cb = onAfterClose
                onAfterClose = null
                preload(context)
                cb?.invoke()
            }
        }
    }

    /** Verifica consentimiento + premium de forma síncrona. */
    private fun canServeAds(): Boolean {
        val consentOk = try {
            AdsConsentManager.canRequestAds.value
        } catch (_: Throwable) { false }

        val isPremium = try {
            (ConfigurationManager.esPremium as? StateFlow<Boolean>)?.value ?: false
        } catch (_: Throwable) { false }

        return consentOk && !isPremium
    }

    /** Política de frecuencia: intervalo mínimo + tope diario. */
    private fun isWithinPolicy(context: Context): Boolean {
        if (MIN_INTERVAL_MS <= 0L && MAX_PER_DAY >= 9999) return true // modo "siempre"

        val prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_MS, 0L)
        if (now - last < MIN_INTERVAL_MS) return false

        val todayKey = todayKey()
        val savedDay = prefs.getString(KEY_DAY, null)
        val count = if (todayKey == savedDay) prefs.getInt(KEY_COUNT_DAY, 0) else 0
        if (count >= MAX_PER_DAY) return false

        return true
    }

    /** Guarda el último momento y el conteo del día. */
    private fun markShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE)
        val todayKey = todayKey()
        val savedDay = prefs.getString(KEY_DAY, null)
        val currentCount = if (todayKey == savedDay) prefs.getInt(KEY_COUNT_DAY, 0) else 0

        prefs.edit()
            .putLong(KEY_LAST_MS, System.currentTimeMillis())
            .putString(KEY_DAY, todayKey)
            .putInt(KEY_COUNT_DAY, currentCount + 1)
            .apply()
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
}


//// app/src/main/java/es/nuskysoftware/marketsales/ads/AdsInterstitialController.kt
//package es.nuskysoftware.marketsales.ads
//
//import android.app.Activity
//import android.content.Context
//import android.content.Context.MODE_PRIVATE
//import android.util.Log
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.FullScreenContentCallback
//import com.google.android.gms.ads.interstitial.InterstitialAd
//import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
//import com.google.android.gms.ads.AdError
//import com.google.android.gms.ads.LoadAdError
//import com.google.firebase.BuildConfig
////import es.nuskysoftware.marketsales.BuildConfig
//import es.nuskysoftware.marketsales.R
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import kotlinx.coroutines.flow.StateFlow
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.concurrent.atomic.AtomicBoolean
//
///**
// * Interstitial unificado:
// * - Usa test unit en DEBUG y el id real en release.
// * - Respeta consentimiento (UMP) y estado Premium.
// * - Control de frecuencia (intervalo mínimo + tope diario).
// * - Precarga automática tras mostrar/fallar.
// *
// * Uso típico:
// *   AdsInterstitialController.preload(context)
// *   AdsInterstitialController.maybeShow(activity) { continuar() }
// */
//object AdsInterstitialController {
//
//    private const val TAG = "AdsInterstitial"
//
//    // ======= POLÍTICA DE FRECUENCIA =======
//    /** Si quieres que salga "siempre", pon 0L (no recomendado para publicación). */
//    private const val MIN_INTERVAL_MS = 0L        // p.ej. 30_000L para 30s
//    /** Tope diario. Sube/baja según tus pruebas (¡ojo con políticas!). */
//    private const val MAX_PER_DAY = 9999          // p.ej. 6 para producción
//
//    // ======= PREFERENCIAS (conteo/fecha) =======
//    private const val PREFS = "ads_prefs"
//    private const val KEY_LAST_MS = "inter_last_ms"
//    private const val KEY_DAY = "inter_day"
//    private const val KEY_COUNT_DAY = "inter_count_day"
//
//    @Volatile private var interstitial: InterstitialAd? = null
//    private val isLoading = AtomicBoolean(false)
//    @Volatile private var onAfterClose: (() -> Unit)? = null
//
//    private fun adUnitId(context: Context): String =
//        if (BuildConfig.DEBUG) {
//            // Test ID oficial de Google para Interstitials
//            "ca-app-pub-3940256099942544/1033173712"
//        } else {
//            context.getString(R.string.admob_interstitial_id)
//        }
//
//    /** Llama tras MobileAds.initialize() y cada vez que quieras tener uno preparado. */
//    fun preload(context: Context) {
//        if (isLoading.get() || interstitial != null) return
//        if (!canServeAds()) return
//
//        val appCtx = context.applicationContext
//        val unitId = adUnitId(appCtx)
//
//        isLoading.set(true)
//        InterstitialAd.load(
//            appCtx,
//            unitId,
//            AdRequest.Builder().build(),
//            object : InterstitialAdLoadCallback() {
//                override fun onAdLoaded(ad: InterstitialAd) {
//                    interstitial = ad
//                    isLoading.set(false)
//                    attachCallbacks(appCtx, ad)
//                    Log.d(TAG, "Interstitial cargado.")
//                }
//
//                override fun onAdFailedToLoad(error: LoadAdError) {
//                    interstitial = null
//                    isLoading.set(false)
//                    Log.w(TAG, "Fallo al cargar interstitial: ${error.message}")
//                }
//            }
//        )
//    }
//
//    /**
//     * Muestra el interstitial si:
//     *  - hay consentimiento UMP,
//     *  - NO es premium,
//     *  - respeta intervalo mínimo y tope diario,
//     *  - y está precargado.
//     * Si no cumple, ejecuta inmediatamente [onFinished].
//     */
//    fun maybeShow(activity: Activity, onFinished: () -> Unit = {}) {
//        if (!canServeAds() || !isWithinPolicy(activity)) {
//            preload(activity)
//            onFinished()
//            return
//        }
//
//        val ad = interstitial
//        if (ad == null) {
//            preload(activity)
//            onFinished()
//            return
//        }
//
//        // Marca uso y registra callback de cierre
//        markShown(activity)
//        onAfterClose = onFinished
//        ad.show(activity)
//    }
//
//    // ======= Internos =======
//
//    private fun attachCallbacks(context: Context, ad: InterstitialAd) {
//        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
//            override fun onAdShowedFullScreenContent() {
//                Log.d(TAG, "Interstitial mostrado.")
//                interstitial = null // invalidar referencia para forzar nueva precarga
//            }
//
//            override fun onAdDismissedFullScreenContent() {
//                Log.d(TAG, "Interstitial cerrado.")
//                val cb = onAfterClose
//                onAfterClose = null
//                preload(context)
//                cb?.invoke()
//            }
//
//            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                Log.w(TAG, "Error al mostrar interstitial: ${adError.message}")
//                val cb = onAfterClose
//                onAfterClose = null
//                preload(context)
//                cb?.invoke()
//            }
//        }
//    }
//
//    /** Verifica consentimiento + premium de forma síncrona. */
//    private fun canServeAds(): Boolean {
//        val consentOk = try {
//            AdsConsentManager.canRequestAds.value
//        } catch (_: Throwable) { false }
//
//        val isPremium = try {
//            (ConfigurationManager.esPremium as? StateFlow<Boolean>)?.value ?: false
//        } catch (_: Throwable) { false }
//
//        return consentOk && !isPremium
//    }
//
//    /** Política de frecuencia: intervalo mínimo + tope diario. */
//    private fun isWithinPolicy(context: Context): Boolean {
//        if (MIN_INTERVAL_MS <= 0L && MAX_PER_DAY >= 9999) return true // modo "siempre"
//
//        val prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE)
//        val now = System.currentTimeMillis()
//        val last = prefs.getLong(KEY_LAST_MS, 0L)
//        if (now - last < MIN_INTERVAL_MS) return false
//
//        val todayKey = todayKey()
//        val savedDay = prefs.getString(KEY_DAY, null)
//        val count = if (todayKey == savedDay) prefs.getInt(KEY_COUNT_DAY, 0) else 0
//        if (count >= MAX_PER_DAY) return false
//
//        return true
//    }
//
//    /** Guarda el último momento y el conteo del día. */
//    private fun markShown(context: Context) {
//        val prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE)
//        val todayKey = todayKey()
//        val savedDay = prefs.getString(KEY_DAY, null)
//        val currentCount = if (todayKey == savedDay) prefs.getInt(KEY_COUNT_DAY, 0) else 0
//
//        prefs.edit()
//            .putLong(KEY_LAST_MS, System.currentTimeMillis())
//            .putString(KEY_DAY, todayKey)
//            .putInt(KEY_COUNT_DAY, currentCount + 1)
//            .apply()
//    }
//
//    private fun todayKey(): String =
//        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
//}
//
//
