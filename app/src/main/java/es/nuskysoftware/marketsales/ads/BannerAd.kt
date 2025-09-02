// app/src/main/java/es/nuskysoftware/marketsales/ads/BannerAd.kt
package es.nuskysoftware.marketsales.ads

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner adaptativo anclado abajo.
 * Crea un AdView nuevo cuando cambian el ID o el ancho para fijar el tamaño SOLO una vez.
 */
@Composable
fun BannerAdBottom(
    modifier: Modifier = Modifier,
    adUnitId: String, // usa el TEST ID de Google en desarrollo
) {
    val context = LocalContext.current
    val cfg = LocalConfiguration.current
    val adWidthDp = cfg.screenWidthDp.coerceAtLeast(320)

    // Re-crear el AdView si cambian el ID o el ancho (no reasignar tamaño en la misma instancia)
    val adView = remember(adUnitId, adWidthDp) {
        AdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setAdUnitId(adUnitId) // solo una vez por instancia
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)) // solo una vez
            loadAd(AdRequest.Builder().build())
        }
    }

    // Liberar correctamente
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView }
    )
}


//package es.nuskysoftware.marketsales.ads
//
//import android.content.Context
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalConfiguration
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.viewinterop.AndroidView
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.AdSize
//import com.google.android.gms.ads.AdView
//import es.nuskysoftware.marketsales.BuildConfig
//
///**
// * Banner adaptativo anclado al borde inferior.
// * - En DEBUG usa el ID de PRUEBA oficial de Google.
// * - En RELEASE usa tu bloque real (parámetro por defecto).
// */
//@Composable
//fun BannerAdBottom(
//    modifier: Modifier = Modifier,
//    adUnitIdRelease: String = "ca-app-pub-9343856188038526/5904335194", // tu bloque real (con '/')
//) {
//    val context = LocalContext.current
//    val cfg = LocalConfiguration.current
//    val adWidthDp = cfg.screenWidthDp.coerceAtLeast(320)
//
//    // Elegimos ID según build
//    val adUnitId = if (BuildConfig.DEBUG)
//        "ca-app-pub-3940256099942544/6300978111"  // TEST banner oficial
//    else
//        adUnitIdRelease
//
//    // Mantener una única instancia del AdView mientras el Composable viva
//    val adView = remember {
//        AdView(context).apply {
//            setAdUnitId(adUnitId)
//            setAdSize(adaptiveSize(context, adWidthDp))
//            loadAd(AdRequest.Builder().build())
//        }
//    }
//
//    // Reajustar tamaño/ID y recargar cuando cambian
//    LaunchedEffect(adUnitId, adWidthDp) {
//        adView.setAdUnitId(adUnitId)
//        adView.setAdSize(adaptiveSize(context, adWidthDp))
//        adView.loadAd(AdRequest.Builder().build())
//    }
//
//    // Liberar el AdView al salir de composición
//    DisposableEffect(Unit) {
//        onDispose { adView.destroy() }
//    }
//
//    AndroidView(
//        modifier = modifier.fillMaxWidth(),
//        factory = { adView }
//    )
//}
//
//private fun adaptiveSize(context: Context, widthDp: Int): AdSize {
//    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
//}
