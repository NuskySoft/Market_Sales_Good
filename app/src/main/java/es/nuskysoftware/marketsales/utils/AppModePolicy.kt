package es.nuskysoftware.marketsales.utils

// Responsabilidad: Define las modalidades Demo, gratuita y Premium, los límites y la autorización de sincronización.

/** Product rules independent of Android and Firebase. */
enum class AppMode { DEMO, FREE, PREMIUM }
object AppModePolicy {
    // Cupos de la modalidad gratuita; la Demo mantiene sus propios límites.
    const val FREE_CATALOG_UNITS = 10L
    const val FREE_MARKETS_YEAR = 50
    fun catalogRemaining(sold: Long): Long = (FREE_CATALOG_UNITS - sold).coerceIn(0L, FREE_CATALOG_UNITS)
    fun canUseCatalog(mode: AppMode, sold: Long, requested: Long): Boolean =
        requested <= 0L || mode == AppMode.PREMIUM || (mode == AppMode.FREE && requested <= catalogRemaining(sold))

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
    // Comprueba si se permite sincronizar sin mezclar datos Demo con cuentas registradas.
    fun canSync(uid: String?, authenticatedUid: String?) = !isLocalOnly(uid) && uid == authenticatedUid
    fun canRefund(mode: AppMode) = mode != AppMode.DEMO
    fun canAssignBalance(mode: AppMode) = mode != AppMode.DEMO
}
