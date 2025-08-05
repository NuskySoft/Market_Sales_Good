package es.nuskysoftware.marketsales

import android.annotation.SuppressLint
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.nuskysoftware.marketsales.data.repository.ConfiguracionRepository
import es.nuskysoftware.marketsales.ui.navigation.addArqueoGraph
import es.nuskysoftware.marketsales.ui.pantallas.PantallaAltaMercadillo
import es.nuskysoftware.marketsales.ui.pantallas.PantallaArticulos
import es.nuskysoftware.marketsales.ui.pantallas.PantallaCambio
import es.nuskysoftware.marketsales.ui.pantallas.PantallaCategorias
import es.nuskysoftware.marketsales.ui.pantallas.PantallaConfiguracion
import es.nuskysoftware.marketsales.ui.pantallas.PantallaEnviarRecibo
import es.nuskysoftware.marketsales.ui.pantallas.PantallaLogin
import es.nuskysoftware.marketsales.ui.pantallas.PantallaMercadillos
import es.nuskysoftware.marketsales.ui.pantallas.PantallaMetodoPago
import es.nuskysoftware.marketsales.ui.pantallas.PantallaPerfil
import es.nuskysoftware.marketsales.ui.pantallas.PantallaResumenVentas
import es.nuskysoftware.marketsales.ui.pantallas.PantallaVentas
import es.nuskysoftware.marketsales.ui.pantallas.PantallaVentasCarrito
import es.nuskysoftware.marketsales.ui.pantallas.PantallaSplash
import es.nuskysoftware.marketsales.ui.pantallas.PantallaLogoutSplash
import es.nuskysoftware.marketsales.ui.theme.MarketSalesTheme
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModelFactory
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager

class MainActivity : ComponentActivity() {

    companion object { private const val TAG = "MainActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "🚀 MainActivity iniciada")

        setContent {
            val configuracionRepository = ConfiguracionRepository(this@MainActivity)
            val configuracionViewModel: ConfiguracionViewModel = viewModel(
                factory = ConfiguracionViewModelFactory(configuracionRepository)
            )

            val isDarkTheme by ConfigurationManager.temaOscuro.collectAsState()

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

@SuppressLint("UnrememberedGetBackStackEntry")
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
        // 👉 Inicio SIEMPRE en "mercadillos". El login es opcional.
        NavHost(navController = navController, startDestination = "mercadillos") {

            // LOGOUT (pantalla splash de logout)
            composable("logout") {
                PantallaLogoutSplash(navController)
            }

            // LISTA MERCADILLOS (home)
            composable("mercadillos") {
                PantallaMercadillos(navController)
            }

            // LOGIN (se entra solo si el usuario lo elige)
            composable("login") {
                PantallaLogin(
                    onNavigateToMain = { navController.popBackStack() }
                )
            }

            composable("configuracion") { PantallaConfiguracion(navController) }
            composable("perfil") { PantallaPerfil(navController = navController) }
            composable("categorias") { PantallaCategorias(navController = navController) }
            composable("articulos") { PantallaArticulos(navController = navController) }

            composable("inventario") {
                es.nuskysoftware.marketsales.ui.pantallas.PantallaInventario(navController)
            }
            composable("listados") {
                es.nuskysoftware.marketsales.ui.pantallas.PantallaListados(navController)
            }

            composable("alta_mercadillo") { PantallaAltaMercadillo(navController = navController) }

            composable(
                route = "editar_mercadillo/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId")
                PantallaAltaMercadillo(
                    navController = navController,
                    mercadilloId = mercadilloId
                )
            }

            // VENTAS
            composable(
                route = "ventas/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId")
                if (mercadilloId != null) {
                    PantallaVentasWrapper(
                        navController = navController,
                        mercadilloId = mercadilloId
                    )
                }
            }

            // MÉTODO DE PAGO
            composable(
                route = "metodo_pago/{mercadilloId}/{totalFmt}",
                arguments = listOf(
                    navArgument("mercadilloId") { type = NavType.StringType },
                    navArgument("totalFmt") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""

                es.nuskysoftware.marketsales.ui.pantallas.PantallaMetodoPago(
                    totalFormateado = totalFmt,
                    onMetodoSeleccionado = { metodo ->
                        when (metodo) {
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.EFECTIVO -> {
                                navController.navigate(
                                    "cambio/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}"
                                )
                            }
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.BIZUM -> {
                                navController.navigate(
                                    "enviar_recibo/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}/bizum"
                                )
                            }
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.TARJETA -> {
                                navController.navigate(
                                    "enviar_recibo/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}/tarjeta"
                                )
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // CAMBIO (efectivo)
            composable(
                route = "cambio/{mercadilloId}/{totalFmt}",
                arguments = listOf(
                    navArgument("mercadilloId") { type = NavType.StringType },
                    navArgument("totalFmt") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
                es.nuskysoftware.marketsales.ui.pantallas.PantallaCambio(
                    totalFormateado = totalFmt,
                    onBack = { navController.popBackStack() },
                    onConfirmarCambio = {
                        navController.navigate(
                            "enviar_recibo/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}/efectivo"
                        )
                    }
                )
            }

            // ENVIAR RECIBO
            composable(
                route = "enviar_recibo/{mercadilloId}/{totalFmt}/{metodo}",
                arguments = listOf(
                    navArgument("mercadilloId") { type = NavType.StringType },
                    navArgument("totalFmt") { type = NavType.StringType },
                    navArgument("metodo") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
                val metodo = backStackEntry.arguments?.getString("metodo") ?: ""

                es.nuskysoftware.marketsales.ui.pantallas.PantallaEnviarRecibo(
                    totalFormateado = totalFmt,
                    metodo = metodo,
                    onBack = { navController.popBackStack() },
                    onEnviar = { /* TODO: envío */ },
                    onFinalizarVenta = {
                        val ventasRoute = "ventas/$mercadilloId"
                        val ventasEntry = navController.getBackStackEntry(ventasRoute)
                        ventasEntry.savedStateHandle["finalizar_metodo"] = metodo
                        navController.popBackStack(route = ventasRoute, inclusive = false)
                    }
                )
            }

            // CARRITO
            composable(
                route = "carrito/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId"){ type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable
                val parent = navController.getBackStackEntry("ventas/$mercadilloId")
                val context = LocalContext.current
                val ventasVM: es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel =
                    viewModel(parent, factory = es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModelFactory(context))

                es.nuskysoftware.marketsales.ui.pantallas.PantallaVentasCarrito(
                    navController = navController,
                    ventasViewModel = ventasVM
                )
            }

            // RESUMEN
            composable(
                route = "resumen/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable

                // 👇 Detecta si vienes desde Arqueo mirando la entrada previa del backstack
                val cameFromArqueo = navController.previousBackStackEntry
                    ?.destination
                    ?.route
                    ?.startsWith("arqueo") == true

                es.nuskysoftware.marketsales.ui.pantallas.PantallaResumenVentas(
                    mercadilloId = mercadilloId,
                    onBack = { navController.popBackStack() },
                    mostrarAbono = !cameFromArqueo // ✅ solo mostramos Abono si NO vienes de Arqueo
                )
            }

            // 🔗 Subgrafo de ARQUEO (rutas: arqueo/*)
            addArqueoGraph(navController)
        }
    }
}

@Composable
fun PantallaVentasWrapper(
    navController: androidx.navigation.NavController,
    mercadilloId: String
) {
    val context = LocalContext.current
    val mercadilloViewModel: es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel = viewModel(
        factory = MercadilloViewModelFactory(context)
    )

    LaunchedEffect(mercadilloId) {
        mercadilloViewModel.cargarMercadillo(mercadilloId)
    }

    val mercadilloParaEditar by mercadilloViewModel.mercadilloParaEditar.collectAsState()

    mercadilloParaEditar?.let { mercadillo ->
        es.nuskysoftware.marketsales.ui.pantallas.PantallaVentas(
            navController = navController,
            mercadilloActivo = mercadillo
        )
    }
}

