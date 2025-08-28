package es.nuskysoftware.marketsales

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import es.nuskysoftware.marketsales.ui.pantallas.PantallaUtilidades
import es.nuskysoftware.marketsales.ui.pantallas.PantallaSaldosPendientes
import es.nuskysoftware.marketsales.ui.pantallas.PantallaDatosEmpresa
import es.nuskysoftware.marketsales.ui.theme.MarketSalesTheme
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModelFactory
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.ui.pantallas.PantallaSplashDescarga


class MainActivity : ComponentActivity() {

    companion object { private const val TAG = "MainActivity" }

    @RequiresApi(Build.VERSION_CODES.O)
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

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnrememberedGetBackStackEntry", "ContextCastToActivity")
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


            composable("utilidades") {
                PantallaUtilidades(navController)
            }
            composable("saldos_pendientes") {
                PantallaSaldosPendientes(navController)
            }
            composable("datos_empresa") {
                PantallaDatosEmpresa(navController)
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
            // Gastos
            composable(
                route = "gastos/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable
                PantallaGastosWrapper(navController = navController, mercadilloId = mercadilloId)
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
            val RUTA_POR_DEFECTO_DESPUES_SPLASH = "mercadillos" // cámbialo si tu ruta es otra

            @Composable
            fun PantallaSplashDescarga(navController: NavController) {
                PantallaSplashDescarga(
                    navController = navController,
                    destinoDespues = RUTA_POR_DEFECTO_DESPUES_SPLASH
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
// --- bloque de navegación: CARRITO GASTOS ---
            composable(
                route = "carrito_gastos/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId"){ type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable
                val context = LocalContext.current

                // 🔐 Owner robusto: gastos/{id} → previous → mercadillos
                val owner =
                    navController.tryGetBackStackEntrySafe("gastos/$mercadilloId")
                        ?: navController.previousBackStackEntry
                        ?: navController.getBackStackEntry("mercadillos")

                val gastosVM: es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel =
                    viewModel(owner, factory = es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory(context))

                es.nuskysoftware.marketsales.ui.pantallas.gastos.PantallaGastosCarrito(
                    navController = navController,
                    gastosViewModel = gastosVM
                )
            }

            // MÉTODO DE PAGO GASTOS (sin pantalla de cambio/recibo)
            composable(
                route = "metodo_pago_gastos/{mercadilloId}/{totalFmt}",
                arguments = listOf(
                    navArgument("mercadilloId") { type = NavType.StringType },
                    navArgument("totalFmt") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
                val activity = LocalContext.current as ComponentActivity
                val gastosVM: es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel =
                    viewModel(activity, factory = es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory(activity.applicationContext))

                es.nuskysoftware.marketsales.ui.pantallas.PantallaMetodoPago(
                    totalFormateado = totalFmt,
                    onMetodoSeleccionado = { metodo ->
                        val metodoStr = when (metodo) {
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.EFECTIVO -> "efectivo"
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.BIZUM -> "bizum"
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.TARJETA -> "tarjeta"
                        }
                        gastosVM.cargarGastos(mercadilloId, metodoStr) {
                            navController.popBackStack(route = "mercadillos", inclusive = false)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }


// app/src/main/java/es/nuskysoftware/marketsales/MainActivity.kt  (bloque de ruta: carrito_gastos)
            composable(
                route = "carrito_gastos/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId"){ type = NavType.StringType })
            ) { backStackEntry ->
                val activity = LocalContext.current as ComponentActivity
                val gastosVM: es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel =
                    viewModel(activity, factory = es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory(activity.applicationContext))

                es.nuskysoftware.marketsales.ui.pantallas.gastos.PantallaGastosCarrito(
                    navController = navController,
                    gastosViewModel = gastosVM
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
@Composable
fun PantallaGastosWrapper(
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
        es.nuskysoftware.marketsales.ui.pantallas.gastos.PantallaGastos(
            navController = navController,
            mercadilloActivo = mercadillo
        )
    }
}
// helper seguro para evitar IllegalArgumentException al buscar entries
private fun androidx.navigation.NavController.tryGetBackStackEntrySafe(route: String)
        : androidx.navigation.NavBackStackEntry? =
    try { getBackStackEntry(route) } catch (_: IllegalArgumentException) { null }
