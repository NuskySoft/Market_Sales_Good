package es.nuskysoftware.marketsales.utils
import android.content.Context
import android.net.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class ConnectivityObserver(context: Context) : java.io.Closeable {
    companion object {
        private val state = MutableStateFlow(false)
        @Volatile private var started = false
        @Synchronized private fun start(context: Context) {
            if (started) return
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            fun refresh() { state.value = cm.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } == true }
            refresh()
            cm.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { refresh() }
                override fun onLost(network: Network) { refresh() }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { refresh() }
            })
            started = true
        }
    }
    init { start(context) }
    val isConnected: StateFlow<Boolean> get() = state
    // Callback intentionally lives for the application process, not a screen or repository.
    override fun close() { }
}
