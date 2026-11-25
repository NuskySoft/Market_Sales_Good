package es.nuskysoftware.marketsales.utils

import android.content.Context

/** Compatibility entry point. Demo data never comes from Firebase. */
object DefaultBootstrapper {
    @Deprecated("Guest cloud bootstrap has been retired; Demo is local only")
    suspend fun runIfNeeded(context: Context) { /* intentionally no-op */ }
}
