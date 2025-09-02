// app/src/main/java/es/nuskysoftware/marketsales/ads/AdsConsentManager.kt
package es.nuskysoftware.marketsales.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor centralizado de consentimiento (Google UMP).
 * - No fuerza geografía en debug (no abre siempre).
 * - Muestra el formulario SOLO si es REQUIRED.
 *
 * MainActivity espera:
 *  - AdsConsentManager.init(context)
 *  - AdsConsentManager.requestConsentAndShowFormIfRequired(activity) { ... }
 */
object AdsConsentManager {

    private lateinit var consentInformation: ConsentInformation

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _lastError = MutableStateFlow<FormError?>(null)
    val lastError: StateFlow<FormError?> = _lastError.asStateFlow()

    // Para pruebas internas puedes añadir IDs hasheadas aquí; por defecto vacío.
    private val debugTestDeviceIds: List<String> = emptyList()

    // Si quieres forzar geografía en debug, asigna EEA/NOT_EEA; por defecto null (no forzar).
    private val debugGeography: Int? = null
    // p.ej.: ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA

    /** Inicializa el objeto; no solicita formulario ni lo muestra. */
    fun init(context: Context) {
        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        _canRequestAds.value = consentInformation.canRequestAds()
    }

    /**
     * Actualiza el estado de consentimiento y muestra el formulario SOLO si es REQUIRED.
     * Llama a [onComplete] cuando:
     *  - No es necesario formulario, o
     *  - El usuario cierra el formulario (con o sin cambio).
     */
    fun requestConsentAndShowFormIfRequired(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        if (!::consentInformation.isInitialized) {
            consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        }

        val params = buildRequestParameters(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                _canRequestAds.value = consentInformation.canRequestAds()
                if (consentInformation.isConsentFormAvailable) {
                    loadAndMaybeShowForm(activity, onComplete)
                } else {
                    onComplete()
                }
            },
            { error ->
                _lastError.value = error
                _canRequestAds.value = consentInformation.canRequestAds()
                onComplete()
            }
        )
    }

    /** Muestra las opciones de privacidad bajo demanda (botón en Ajustes). */
    fun showPrivacyOptions(activity: Activity, onDismissed: (() -> Unit)? = null) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error: FormError? ->
            if (error != null) _lastError.value = error
            _canRequestAds.value = consentInformation.canRequestAds()
            onDismissed?.invoke()
        }
    }

    /** Reintenta actualizar el estado (sin forzar mostrar formulario). */
    fun refresh(activity: Activity, onDone: (() -> Unit)? = null) {
        if (!::consentInformation.isInitialized) {
            consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        }
        val params = buildRequestParameters(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                _canRequestAds.value = consentInformation.canRequestAds()
                onDone?.invoke()
            },
            { error ->
                _lastError.value = error
                _canRequestAds.value = consentInformation.canRequestAds()
                onDone?.invoke()
            }
        )
    }

    /** Solo para pruebas: limpia el estado local de consentimiento. */
    fun reset(context: Context) {
        if (!::consentInformation.isInitialized) {
            consentInformation = UserMessagingPlatform.getConsentInformation(context)
        }
        consentInformation.reset()
        _canRequestAds.value = false
        _lastError.value = null
    }

    // --- Internos ---

    private fun loadAndMaybeShowForm(activity: Activity, onComplete: () -> Unit) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form: ConsentForm ->
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    form.show(activity) { error: FormError? ->
                        if (error != null) _lastError.value = error
                        _canRequestAds.value = consentInformation.canRequestAds()
                        onComplete()
                    }
                } else {
                    _canRequestAds.value = consentInformation.canRequestAds()
                    onComplete()
                }
            },
            { error: FormError ->
                _lastError.value = error
                _canRequestAds.value = consentInformation.canRequestAds()
                onComplete()
            }
        )
    }

    private fun buildRequestParameters(context: Context): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG && (debugTestDeviceIds.isNotEmpty() || debugGeography != null)) {
            val dbg = ConsentDebugSettings.Builder(context).apply {
                debugTestDeviceIds.forEach { addTestDeviceHashedId(it) }
                debugGeography?.let { setDebugGeography(it) }
            }.build()
            builder.setConsentDebugSettings(dbg)
        }

        return builder.build()
    }
}
