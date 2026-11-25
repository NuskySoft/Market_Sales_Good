package es.nuskysoftware.marketsales.utils

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
    fun canSync(uid: String?): Boolean = AppModePolicy.canSync(uid, FirebaseAuth.getInstance().currentUser?.uid)
    suspend fun mayRestore(db: es.nuskysoftware.marketsales.data.local.database.AppDatabase, uid: String): Boolean {
        if (!canSync(uid)) return false
        if (ConfigurationManager.getIsPremium()) return true
        return db.categoriaDao().existeAlgunoDeUsuario(uid) || db.articuloDao().existeAlgunoDeUsuario(uid) ||
            db.mercadilloDao().existeAlgunoDeUsuario(uid) || db.recibosDao().existeAlgunoDeUsuario(uid) ||
            db.lineasGastosDao().existeAlgunoDeUsuario(uid) || db.lineasVentaDao().existeAlgunoDeUsuario(uid)
    }
    fun requireOwner(uid: String?) {
        check(!uid.isNullOrBlank() && uid == ConfigurationManager.getCurrentUserId()) { "Session changed" }
    }
    fun requireRegistered(key: String = "demo_solo_registrados") {
        if (isDemo()) deny(key)
    }
    // Keys: demo_limite_ventas, demo_limite_mercadillos, demo_limite_categorias, demo_limite_articulos.
    fun checkLimit(resource: String, count: Int) {
        AppModePolicy.limitKey(mode(), resource, count)?.let { deny(it) }
    }
    fun deny(key: String): Nothing {
        show(key)
        throw IllegalStateException(StringResourceManager.getString(key, ConfigurationManager.idioma.value))
    }
}
