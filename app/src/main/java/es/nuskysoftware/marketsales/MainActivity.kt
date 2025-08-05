// app/src/main/java/es/nuskysoftware/marketsales/MainActivity.kt
package es.nuskysoftware.marketsales

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.nuskysoftware.marketsales.data.local.entity.isPremium
import es.nuskysoftware.marketsales.data.local.entity.modoOscuro
import es.nuskysoftware.marketsales.ui.pantallas.PantallaConfiguracion
import es.nuskysoftware.marketsales.ui.pantallas.PantallaLogin
import es.nuskysoftware.marketsales.ui.pantallas.PantallaMercadillos
import es.nuskysoftware.marketsales.ui.pantallas.PantallaSplash
import es.nuskysoftware.marketsales.ui.theme.MarketSalesTheme
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.data.repository.ConfiguracionRepository

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "🚀 MainActivity iniciada")

        setContent {
            // --- tu ViewModel y repo exactos ---
            val configuracionRepository = ConfiguracionRepository(this@MainActivity)
            val configuracionViewModel: ConfiguracionViewModel = viewModel(
                factory = ConfiguracionViewModelFactory(configuracionRepository)
            )

            // 1) Leemos de Room la entidad de configuración
            val configuracion by configuracionViewModel.configuracion.collectAsState()

            // 2) Cuando cambie, volcamos todo en ConfigurationManager
            LaunchedEffect(configuracion) {
                configuracion?.let { config ->
                    ConfigurationManager.updateConfiguration(
                        idioma     = config.idioma,
                        fuente     = config.fuente,
                        modoOscuro = config.modoOscuro,
                        isPremium  = config.isPremium
                    )
                }
            }

            // 3) Aquí capturamos el flag de tema oscuro para la recomposición de la UI
            val isDarkTheme by ConfigurationManager.temaOscuro.collectAsState()

            // 4) Pasamos ese flag a nuestro tema sin cambiar una sola pantalla
            MarketSalesTheme(configurationManager = ConfigurationManager) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationSystem(configuracionViewModel)
                }
            }
        }
    }
}

@Composable
fun NavigationSystem(
    configuracionViewModel: ConfiguracionViewModel
) {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSplash = false
    }

    if (showSplash) {
        PantallaSplash()
    } else {
        NavHost(navController = navController, startDestination = "mercadillos") {
            composable("mercadillos") {
                PantallaMercadillos(navController, configuracionViewModel)
            }
            composable("configuracion") {
                PantallaConfiguracion(navController)
            }
            composable("login") {
                PantallaLogin(
                    onNavigateToMain = {
                        navController.navigate("mercadillos") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

