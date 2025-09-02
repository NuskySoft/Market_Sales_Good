package es.nuskysoftware.marketsales.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.utils.ConfigurationManager

/**
 * Slot reutilizable para mostrar el banner de anuncios en el bottomBar.
 * Solo se muestra si:
 *  - hay consentimiento válido (UMP),
 *  - el usuario NO es premium.
 * Lee el ID del bloque desde recursos (debug=prueba, release=real).
 */
@Composable
fun AdsBottomBar() {
    val canRequestAds by AdsConsentManager.canRequestAds.collectAsState()
    val esPremium by ConfigurationManager.esPremium.collectAsState()

    if (canRequestAds && !esPremium) {
        BannerAdBottom(
            adUnitId = stringResource(id = R.string.admob_banner_id)
        )
    }
}
