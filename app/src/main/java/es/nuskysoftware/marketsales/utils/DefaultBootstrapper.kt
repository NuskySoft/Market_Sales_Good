package es.nuskysoftware.marketsales.utils

// Responsabilidad: Agrupa la inicialización de datos por defecto utilizada por la aplicación.

import android.content.Context

/** Compatibility entry point. Demo data never comes from Firebase. */
object DefaultBootstrapper {
    @Deprecated("Guest cloud bootstrap has been retired; Demo is local only")
    suspend fun runIfNeeded(context: Context) { /* intentionally no-op */ }
}
