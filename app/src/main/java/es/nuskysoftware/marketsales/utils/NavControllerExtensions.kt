
// app/src/main/java/es/nuskysoftware/marketsales/utils/NavControllerExtensions.kt
package es.nuskysoftware.marketsales.utils

import android.os.Handler
import android.os.Looper
import androidx.navigation.NavController
import java.util.concurrent.atomic.AtomicBoolean

private const val NAVIGATION_LOCK_TIME = 1000L
private val isNavigating = AtomicBoolean(false)

fun NavController.safePopBackStack(): Boolean {
    if (!isNavigating.compareAndSet(false, true)) return false
    val result = this.popBackStack()
    Handler(Looper.getMainLooper()).postDelayed({ isNavigating.set(false) }, NAVIGATION_LOCK_TIME)
    return result
}

fun NavController.safePopBackStack(route: String, inclusive: Boolean): Boolean {
    if (!isNavigating.compareAndSet(false, true)) return false
    val result = this.popBackStack(route, inclusive)
    Handler(Looper.getMainLooper()).postDelayed({ isNavigating.set(false) }, NAVIGATION_LOCK_TIME)
    return result
}
