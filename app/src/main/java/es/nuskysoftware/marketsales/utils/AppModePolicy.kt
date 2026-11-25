package es.nuskysoftware.marketsales.utils

/** Product rules independent of Android and Firebase. */
enum class AppMode { DEMO, FREE, PREMIUM }
object AppModePolicy {
    const val DEMO_USER_ID = "demo_local_v2"
    fun mode(authenticated: Boolean, premium: Boolean) = when {
        !authenticated -> AppMode.DEMO
        premium -> AppMode.PREMIUM
        else -> AppMode.FREE
    }
    fun isLocalOnly(uid: String?) = uid.isNullOrBlank() || uid == DEMO_USER_ID || uid == "usuario_default"
    fun limitKey(mode: AppMode, resource: String, count: Int): String? {
        if (mode != AppMode.DEMO) return null
        val limit = if (resource == "ventas") 10 else 1
        return if (count >= limit) "demo_limite_$resource" else null
    }
    fun canSell(mode: AppMode, stateAllowsSales: Boolean) = mode == AppMode.DEMO || stateAllowsSales
    fun canSync(uid: String?, authenticatedUid: String?) = !isLocalOnly(uid) && uid == authenticatedUid
    fun canRefund(mode: AppMode) = mode != AppMode.DEMO
    fun canAssignBalance(mode: AppMode) = mode != AppMode.DEMO
}
