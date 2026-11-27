package es.nuskysoftware.marketsales.utils

// Responsabilidad: Expone el estado de configuración y cuenta que observan las pantallas Compose.

import android.content.ContentValues.TAG
import android.util.Log
import es.nuskysoftware.marketsales.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ✅ COMPLETO V10 - ConfigurationManager con TODOS los métodos necesarios
 */
object ConfigurationManager {

    // ========== ESTADOS V10 ==========
    private val _idioma = MutableStateFlow("es")
    private val _fuente = MutableStateFlow("Montserrat")
    private val _temaOscuro = MutableStateFlow(false)
    private val _moneda = MutableStateFlow("€ Euro")
    private val _usuarioLogueado = MutableStateFlow<String?>(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: AppModePolicy.DEMO_USER_ID)
    // O si quieres que el valor inicial cuando nadie está logueado sea null:
    // private val _usuarioLogueado = MutableStateFlow<String?>(null)
    private val _usuarioEmail = MutableStateFlow<String?>(null)
    private val _displayName = MutableStateFlow<String?>(null)
    private val _versionApp = MutableStateFlow(0) // Para compatibilidad

    private val _isAuthenticated = MutableStateFlow(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null)

    @Volatile private var roomAccountOwner: String? = null
    @Volatile private var roomAccountPremium: Boolean? = null

    /** Publish only the committed Room row of the currently selected account. */
    @Synchronized
    fun applyAccountFromRoom(uid: String, premium: Boolean?, email: String? = null, name: String? = null) {
        if (uid != _usuarioLogueado.value) return
        roomAccountOwner = uid
        roomAccountPremium = premium
        if (premium == null) { markPremiumUnknown(); return }
        _esPremium.value = premium
        _versionApp.value = if (premium) 1 else 0
        email?.let { _usuarioEmail.value = it }
        name?.let { _displayName.value = it }
        _esPremiumReady.value = true
    }

    private var lastSuccessfulPremiumState: Boolean? = null

    private var lastUpdateTimestamp = 0L

    // Gate de premium resuelto para evitar interstitials en transición (Solución B)
    private val _esPremiumReady = MutableStateFlow(AppModePolicy.isLocalOnly(_usuarioLogueado.value))
    val esPremiumReady: StateFlow<Boolean> = _esPremiumReady.asStateFlow()

    // ========== ESTADOS PÚBLICOS ==========
    val idioma: StateFlow<String> = _idioma.asStateFlow()
    val fuente: StateFlow<String> = _fuente.asStateFlow()
    val temaOscuro: StateFlow<Boolean> = _temaOscuro.asStateFlow()
    val moneda: StateFlow<String> = _moneda.asStateFlow()
    val usuarioEmail: StateFlow<String?> = _usuarioEmail.asStateFlow()
    val versionApp: StateFlow<Int> = _versionApp.asStateFlow()

    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    val displayName: StateFlow<String?> = _displayName.asStateFlow()
    val usuarioLogueado: StateFlow<String?> = _usuarioLogueado.asStateFlow()

    private val _esPremium = MutableStateFlow(false)
    val esPremium: StateFlow<Boolean> = _esPremium.asStateFlow()
    val versionAppValue: String = BuildConfig.VERSION_NAME

    /**
     * Toggle Premium para desarrollo - SIMPLIFICADO SIN PROTECCIONES
     */
    fun togglePremiumForDevelopment() {
        val newValue = !_esPremium.value

        Log.d("ConfigurationManager", "🔧 TOGGLE PREMIUM (Development):")
        Log.d("ConfigurationManager", "   - Antes: ${_esPremium.value}")
        Log.d("ConfigurationManager", "   - Después: $newValue")

        // Cambio directo sin protecciones para desarrollo
        _esPremium.value = newValue
        _versionApp.value = if (newValue) 1 else 0

        Log.d("ConfigurationManager", "✅ Premium toggled exitosamente")
    }

    /**
     * Establece estado premium directamente (para desarrollo)
     */
    fun setPremiumForDevelopment(isPremium: Boolean) {
        Log.d("ConfigurationManager", "🔧 SET PREMIUM (Development): ${_esPremium.value} → $isPremium")

        _esPremium.value = isPremium
        _versionApp.value = if (_esPremium.value) 1 else 0

        Log.d("ConfigurationManager", "✅ Premium establecido: $isPremium")
    }

    // ======= Gate helpers (Solución B) =======
    fun markPremiumUnknown() {
        // Llamar al iniciar login o cuando aún no sabemos si es premium
        _esPremiumReady.value = false
    }

    fun markPremiumKnown() {
        // Llamar cuando YA sepamos el estado real de premium (éxito o error)
        if (AppModePolicy.isLocalOnly(_usuarioLogueado.value) || (roomAccountOwner == _usuarioLogueado.value && roomAccountPremium != null)) _esPremiumReady.value = true
    }

    // ========== MÉTODOS PRINCIPALES ==========
    fun updateUserConfigurationProtected(
        displayName: String? = null,
        usuarioEmail: String? = null,
        planUsuario: String = "FREE",
        isAuthenticated: Boolean = false,
        source: String = "unknown"
    ) {
        val currentTime = System.currentTimeMillis()

        // ✅ PROTECCIÓN: Si el displayName cambió hace menos de 5 segundos, no sobrescribir
        if (displayName != null && _displayName.value != displayName) {
            if (currentTime - lastUpdateTimestamp < 5000) {
                Log.w("ConfigurationManager", "🚫 PROTECCIÓN: Evitando sobrescribir displayName reciente")
                Log.w("ConfigurationManager", "   - Valor actual: ${_displayName.value}")
                Log.w("ConfigurationManager", "   - Valor que se quiere poner: $displayName")
                Log.w("ConfigurationManager", "   - Tiempo desde último cambio: ${currentTime - lastUpdateTimestamp}ms")
                return
            }
        }

        // Actualizar timestamp si hay cambio de displayName
        if (displayName != null && _displayName.value != displayName) {
            lastUpdateTimestamp = currentTime
            Log.d("ConfigurationManager", "🕒 Actualizando timestamp de protección")
        }

        // Llamar al método normal
        updateUserConfiguration(
            displayName = displayName,
            usuarioEmail = usuarioEmail,
            planUsuario = planUsuario,
            isAuthenticated = isAuthenticated
        )
    }

    /**
     * Obtiene el ID del usuario actual
     */
    // Devuelve el identificador de propietario que utiliza la sesión activa.
    fun getCurrentUserId(): String? {
        return _usuarioLogueado.value
    }

    // ========== MÉTODOS GET ==========
    fun getIdioma(): String = _idioma.value
    fun getFuente(): String = _fuente.value
    fun getModoOscuro(): Boolean = _temaOscuro.value
    fun getTemaOscuro(): Boolean = _temaOscuro.value
    fun getMoneda(): String = _moneda.value
    fun getUsuarioEmail(): String? = _usuarioEmail.value
    fun getUsuarioId(): String? = _usuarioLogueado.value.takeIf { it != es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID }
    fun getVersionApp(): Int = _versionApp.value
    fun getIsPremium(): Boolean = _esPremium.value

    // ========== MÉTODOS DE AUTENTICACIÓN ==========

    /**
     * Login de usuario
     */
    fun login(userId: String, email: String?, displayName: String? = null, isPremium: Boolean = false) {
        setUsuarioLogueado(userId)
        _usuarioEmail.value = email
        _displayName.value = displayName
        //_esPremium.value = isPremium
        setEsPremiumProtected(isPremium, "updateUserConfiguration")

        _versionApp.value = if (_esPremium.value) 1 else 0
        _isAuthenticated.value = true

        // premium resuelto tras login
        markPremiumKnown()
    }


    private fun setEsPremiumProtected(newValue: Boolean, source: String = "unknown") {
        val oldValue = _esPremium.value

        Log.d("ConfigurationManager", "🔄 INTENTO CAMBIO esPremium: $oldValue → $newValue")
        Log.d("ConfigurationManager", "   - Fuente: $source")
        Log.d("ConfigurationManager", "   - lastSuccessfulPremiumState: $lastSuccessfulPremiumState")
        Log.d("ConfigurationManager", "   - isAuthenticated: ${_isAuthenticated.value}")

        // A transport result or fallback must never override a committed local account row.
        _esPremium.value = if (roomAccountOwner == _usuarioLogueado.value && roomAccountPremium != null) roomAccountPremium!! else newValue
        Log.d("ConfigurationManager", "✅ esPremium cambiado: $oldValue → $newValue (fuente: $source)")
    }

    /**
     * Logout de usuario
     */
    fun logout() {
        Log.d("ConfigurationManager", "🚪 logout() llamado!")
        Log.d("ConfigurationManager", "   - esPremium antes: ${_esPremium.value}")
        Log.d("ConfigurationManager", "   - STACK TRACE:")
        Thread.currentThread().stackTrace.take(8).forEach { frame ->
            Log.d("ConfigurationManager", "     at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
        }

        // 🔓 Limpiar estado de protección en logout real
        lastSuccessfulPremiumState = null
        Log.d("ConfigurationManager", "   - Protección premium limpiada")

        _isAuthenticated.value = false
        resetToDefaults()

        Log.d("ConfigurationManager", "   - esPremium después: ${_esPremium.value}")
        Log.d("ConfigurationManager", "   - ✅ Logout completado")

        // premium resuelto (false) tras logout
        markPremiumKnown()
    }

    // ========== MÉTODOS DE CONFIGURACIÓN ==========

    /**
     * Verifica si el usuario puede cambiar configuración
     */
    fun canChangeConfiguration(): Boolean {
        return _esPremium.value
    }

    /**
     * Actualiza configuración global
     */
    fun updateGlobalConfiguration(
        idioma: String,
        fuente: String,
        temaOscuro: Boolean,
        moneda: String
    ) {
        _idioma.value = idioma
        _fuente.value = fuente
        _temaOscuro.value = temaOscuro
        _moneda.value = moneda
    }

    /**
     * Actualiza premium del usuario
     */
    fun updateUserPremium(isPremium: Boolean) {
        setEsPremiumProtected(isPremium, "updateUserConfiguration")
        _versionApp.value = if (_esPremium.value) 1 else 0
    }

    // ========== MÉTODOS UPDATE EXISTENTES ==========
    fun updateConfiguration(
        idioma: String,
        fuente: String,
        modoOscuro: Boolean,
        isPremium: Boolean
    ) {
        _idioma.value = idioma
        _fuente.value = fuente
        _temaOscuro.value = modoOscuro
        setEsPremiumProtected(isPremium, "updateUserConfiguration")
        _versionApp.value = if (_esPremium.value) 1 else 0
    }

    // Añade estos logs TEMPORALES en ConfigurationManager.kt en estos métodos:

    fun updateUserConfiguration(
        idioma: String = _idioma.value,
        fuente: String = _fuente.value,
        temaOscuro: Boolean = _temaOscuro.value,
        usuarioEmail: String? = _usuarioEmail.value,
        usuarioId: String? = null,
        planUsuario: String = "FREE",
        empresaId: String? = null,
        tipoUsuario: String? = null,
        displayName: String? = null,
        photoUrl: String? = null,
        isAuthenticated: Boolean = false,
        moneda: String = _moneda.value ,
    ) {
        // 🔍 LOG TEMPORAL
        Log.d("ConfigurationManager", "🔧 updateUserConfiguration llamado:")
        Log.d("ConfigurationManager", "   - planUsuario: $planUsuario")
        Log.d("ConfigurationManager", "   - isAuthenticated: $isAuthenticated")
        Log.d("ConfigurationManager", "   - esPremium antes: ${_esPremium.value}")

        Log.d("ConfigurationManager", "🔧 updateUserConfiguration - displayName recibido: $displayName")
        Log.d("ConfigurationManager", "🔧 _displayName.value antes: ${_displayName.value}")

        Log.d("ConfigurationManager", "🔧 FINAL updateUserConfiguration:")
        Log.d("ConfigurationManager", "   - displayName: ${_displayName.value}")
        Log.d("ConfigurationManager", "   - isAuthenticated: ${_isAuthenticated.value}")
        Log.d("ConfigurationManager", "   - esPremium: ${_esPremium.value}")

        setUsuarioLogueado(if (isAuthenticated) usuarioId else AppModePolicy.DEMO_USER_ID)
        // Actualizar valores de configuración
        _idioma.value = idioma
        _fuente.value = fuente
        _temaOscuro.value = temaOscuro
        _usuarioEmail.value = usuarioEmail
        _displayName.value = displayName
        _moneda.value = moneda
        _isAuthenticated.value = isAuthenticated

        // Calcular estado premium
        val isPremium = planUsuario == "PREMIUM" && isAuthenticated
        setEsPremiumProtected(isPremium, "updateUserConfiguration")
        _versionApp.value = if (_esPremium.value) 1 else 0

        // 💾 Guardar último estado premium válido para protección
        if (isPremium) {
            lastSuccessfulPremiumState = true
            Log.d("ConfigurationManager", "   - esPremium después: ${_esPremium.value}")
        }
        if (displayName != null) {
            _displayName.value = displayName
        }

        Log.d("ConfigurationManager", "🔧 _displayName.value después: ${_displayName.value}")

        // ... resto del código ...

        // 🔍 LOG TEMPORAL
        Log.d("ConfigurationManager", "   - esPremium después: ${_esPremium.value}")
        Log.d("ConfigurationManager", "   - STACK TRACE:")
        Thread.currentThread().stackTrace.take(8).forEach { frame ->
            Log.d("ConfigurationManager", "     at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
        }

        // premium resuelto tras aplicar configuración de backend
        markPremiumKnown()
    }

    fun forceUpdateDisplayName(newDisplayName: String) {
        Log.d(TAG, "🔧 forceUpdateDisplayName: ${_displayName.value} → $newDisplayName")
        _displayName.value = newDisplayName
        Log.d(TAG, "🔧 forceUpdateDisplayName DESPUÉS: ${_displayName.value}")
    }

    // ========== MÉTODOS SET ==========
    fun setIdioma(idioma: String) {
        _idioma.value = idioma
    }

    fun setFuente(fuente: String) {
        _fuente.value = fuente
    }

    fun setTemaOscuro(temaOscuro: Boolean) {
        _temaOscuro.value = temaOscuro
    }

    fun setModoOscuro(modoOscuro: Boolean) {
        _temaOscuro.value = modoOscuro
    }

    fun setUsuarioEmail(email: String?) {
        _usuarioEmail.value = email
    }

    @Synchronized
    fun setUsuarioLogueado(userId: String?) {
        val next = userId ?: AppModePolicy.DEMO_USER_ID
        if (_usuarioLogueado.value != next) {
            roomAccountOwner = null
            roomAccountPremium = null
            _esPremium.value = false
            _versionApp.value = 0
            _esPremiumReady.value = AppModePolicy.isLocalOnly(next)
        }
        _usuarioLogueado.value = next
    }

    fun setUsuarioId(userId: String?) {
        setUsuarioLogueado(userId)
    }

    fun setMoneda(moneda: String) {
        _moneda.value = moneda
    }

    fun setIsPremium(isPremium: Boolean) {
        setEsPremiumProtected(isPremium, "updateUserConfiguration")
        _versionApp.value = if (_esPremium.value) 1 else 0
    }

    fun setVersionApp(versionApp: Int) {
        _versionApp.value = versionApp
        setEsPremiumProtected(versionApp == 1, "setVersionApp")
    }

    fun resetToDefaults() {
        // 🔍 LOG TEMPORAL
        Log.d("ConfigurationManager", "🔄 resetToDefaults() llamado!")
        Log.d("ConfigurationManager", "   - esPremium antes: ${_esPremium.value}")
        Log.d("ConfigurationManager", "   - lastSuccessfulPremiumState: $lastSuccessfulPremiumState")
        Log.d("ConfigurationManager", "   - STACK TRACE:")
        Thread.currentThread().stackTrace.take(8).forEach { frame ->
            Log.d("ConfigurationManager", "     at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
        }

        // 🛡️ PROTECCIÓN: No resetear si hay un estado premium válido y el usuario está autenticado
        if (lastSuccessfulPremiumState == true && _isAuthenticated.value) {
            Log.w("ConfigurationManager", "🚫 EVITANDO RESET - Usuario premium autenticado detectado")
            return
        }

        // Resetear todos los valores a defaults
        _idioma.value = "es"
        _fuente.value = "Montserrat"
        _temaOscuro.value = false
        _moneda.value = "€ Euro"
        _usuarioLogueado.value = es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID
        _usuarioEmail.value = null
        _displayName.value = null
        setEsPremiumProtected(false, "resetToDefaults")
        _versionApp.value = 0
        _isAuthenticated.value = false

        // Limpiar estado de protección solo en logout real
        lastSuccessfulPremiumState = null

        Log.d("ConfigurationManager", "   - ✅ Reset completado")

        Log.d("ConfigurationManager", "   - esPremium después: ${_esPremium.value}")
        Log.d("ConfigurationManager", "   - ✅ Reset completado")

        // premium resuelto (false) tras reset
        markPremiumKnown()
    }

    // ========== VERIFICACIONES ==========
    fun isUsuarioLogueado(): Boolean {
        return _usuarioLogueado.value != es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID
    }

    fun isPremium(): Boolean = _esPremium.value
    fun isFree(): Boolean = !_esPremium.value
}
