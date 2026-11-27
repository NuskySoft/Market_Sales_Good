package es.nuskysoftware.marketsales.utils

// Responsabilidad: Centraliza comprobaciones de modalidad, propiedad, permisos de sincronización y avisos de límites Demo.

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Central guard. Record ownership is checked even after a session transition. */
object DemoAccess {
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    fun dismiss() { _notice.value = null }
    fun show(key: String) { _notice.value = key }
    fun isDemo() = AppModePolicy.isLocalOnly(ConfigurationManager.getCurrentUserId())
    fun mode() = if (isDemo()) AppMode.DEMO else if (ConfigurationManager.getIsPremium()) AppMode.PREMIUM else AppMode.FREE
    // Comprueba si la cuenta está autorizada para sincronizar con Firebase.
    fun canSync(uid: String?): Boolean = AppModePolicy.canSync(uid, FirebaseAuth.getInstance().currentUser?.uid)
    suspend fun mayRestore(db: es.nuskysoftware.marketsales.data.local.database.AppDatabase, uid: String): Boolean {
        if (!canSync(uid)) return false
        return es.nuskysoftware.marketsales.data.repository.RecoveryGate.currentCanPull(uid)
    }
    // Interrumpe la operación si el propietario no coincide con la sesión actual.
    fun requireOwner(uid: String?) {
        check(!uid.isNullOrBlank() && uid == ConfigurationManager.getCurrentUserId()) { "Session changed" }
    }
    // Bloquea la acción cuando se ejecuta en modalidad Demo.
    fun requireRegistered(key: String = "demo_solo_registrados") {
        if (isDemo()) deny(key)
    }
    /** Bloquea operaciones de gestión profesional fuera de una sesión Premium resuelta. */
    fun requirePremium() {
        if (isDemo() || !ConfigurationManager.esPremiumReady.value || !ConfigurationManager.getIsPremium()) deny("premium_required")
    }
    // Keys: demo_limite_ventas, demo_limite_mercadillos, demo_limite_categorias, demo_limite_articulos.
    // Comprueba el límite correspondiente a la modalidad y recurso.
    fun checkLimit(resource: String, count: Int) {
        AppModePolicy.limitKey(mode(), resource, count)?.let { deny(it) }
    }
    // Publica el aviso y aborta la operación no permitida.
    fun deny(key: String): Nothing {
        show(key)
        throw IllegalStateException(StringResourceManager.getString(key, ConfigurationManager.idioma.value))
    }
}
