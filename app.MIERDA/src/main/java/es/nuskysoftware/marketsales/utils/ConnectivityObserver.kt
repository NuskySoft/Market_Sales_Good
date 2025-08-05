// app/src/main/java/es/nuskysoftware/marketsales/utils/ConnectivityObserver.kt
package es.nuskysoftware.marketsales.utils

import android.content.Context
import android.net.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Observa el estado de la red (online/offline) usando ConnectivityManager
 * y expone un StateFlow<Boolean>.
 */
class ConnectivityObserver(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isConnected = MutableStateFlow(isCurrentlyConnected())
    val isConnected: StateFlow<Boolean> = _isConnected

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
            }
            override fun onLost(network: Network) {
                _isConnected.value = isCurrentlyConnected()
            }
        })
    }

    private fun isCurrentlyConnected(): Boolean {
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
