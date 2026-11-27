// app/src/main/java/es/nuskysoftware/marketsales/ads/BannerAd.kt
package es.nuskysoftware.marketsales.ads

// Responsabilidad: Crea y libera la vista publicitaria adaptativa del pie de pantalla.

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
// Crea la vista adaptativa de banner y vincula su liberación al ciclo de composición.
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

