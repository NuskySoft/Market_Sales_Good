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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.nuskysoftware.marketsales.data.repository.ConfiguracionRepository
import es.nuskysoftware.marketsales.ui.navigation.addArqueoGraph
import es.nuskysoftware.marketsales.ui.pantallas.PantallaAltaMercadillo
import es.nuskysoftware.marketsales.ui.pantallas.PantallaArticulos
import es.nuskysoftware.marketsales.ui.pantallas.PantallaCambio
import es.nuskysoftware.marketsales.ui.pantallas.PantallaCategorias
import es.nuskysoftware.marketsales.ui.pantallas.PantallaConfiguracion
import es.nuskysoftware.marketsales.ui.pantallas.PantallaDatosEmpresa
import es.nuskysoftware.marketsales.ui.pantallas.PantallaEnviarRecibo
import es.nuskysoftware.marketsales.ui.pantallas.PantallaLogin
import es.nuskysoftware.marketsales.ui.pantallas.PantallaMercadillos
import es.nuskysoftware.marketsales.ui.pantallas.PantallaMetodoPago
import es.nuskysoftware.marketsales.ui.pantallas.PantallaPerfil
import es.nuskysoftware.marketsales.ui.pantallas.PantallaResumenVentas
import es.nuskysoftware.marketsales.ui.pantallas.PantallaSaldosPendientes
import es.nuskysoftware.marketsales.ui.pantallas.PantallaSplash
import es.nuskysoftware.marketsales.ui.pantallas.PantallaSplashDescarga
import es.nuskysoftware.marketsales.ui.pantallas.PantallaLogoutSplash
import es.nuskysoftware.marketsales.ui.pantallas.PantallaUtilidades
import es.nuskysoftware.marketsales.ui.pantallas.PantallaVentas
import es.nuskysoftware.marketsales.ui.pantallas.PantallaVentasCarrito
import es.nuskysoftware.marketsales.ui.pantallas.TipoEnvio
import es.nuskysoftware.marketsales.ui.theme.MarketSalesTheme
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModelFactory
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.data.repository.MetodoPago as MetodoPagoRepo
import es.nuskysoftware.marketsales.utils.safePopBackStack
import es.nuskysoftware.marketsales.ads.AdsConsentManager
import com.google.android.gms.ads.MobileAds


import com.google.firebase.Firebase
import es.nuskysoftware.marketsales.ads.AdsInterstitialController   // ← mantiene import

import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

import es.nuskysoftware.marketsales.ui.pantallas.PantallaDebugDatos
import es.nuskysoftware.marketsales.BuildConfig



class MainActivity : ComponentActivity() {

    companion object { private const val TAG = "MainActivity" }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1) Inicializa el gestor UMP
        AdsConsentManager.init(this)

        Log.d(TAG, "🚀 MainActivity iniciada")
        // 2) Pide/actualiza consentimiento y muestra el formulario si hace falta
        AdsConsentManager.requestConsentAndShowFormIfRequired(this) {
            // Se ejecuta cuando el formulario se cierra o no era necesario
            MobileAds.initialize(this) {}
            AdsInterstitialController.preload(this)   // precarga un interstitial
        }

        setContent {
            val configuracionRepository = ConfiguracionRepository(this@MainActivity)
            val configuracionViewModel: ConfiguracionViewModel =
                viewModel(factory = ConfiguracionViewModelFactory(configuracionRepository))

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
fun NavigationSystem(configuracionViewModel: ConfiguracionViewModel) {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    // ▼▼ Mostrar interstitial en cada cambio de pantalla (GATE por premium resuelto) ▼▼
    val activity = LocalContext.current as? ComponentActivity
    val canRequestAds by AdsConsentManager.canRequestAds.collectAsState()
    val esPremium by ConfigurationManager.esPremium.collectAsState()
    val esPremiumReady by ConfigurationManager.esPremiumReady.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(backStackEntry?.destination?.route, canRequestAds, esPremium, esPremiumReady) {
        val route = backStackEntry?.destination?.route ?: return@LaunchedEffect
        // Evitar pantallas delicadas y SOLO si premium ya está resuelto
        val avoidRoutes = setOf("login", "splash_descarga")
        if (activity != null && esPremiumReady && canRequestAds && !esPremium && route !in avoidRoutes) {
            AdsInterstitialController.maybeShow(activity)
        }
    }

    // Si pasa a premium en caliente, limpia cualquier interstitial precargado
    LaunchedEffect(esPremium) {
        if (esPremium) AdsInterstitialController.dropPreloaded()
    }
    // ▲▲ Fin interstitial ▲▲

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSplash = false
    }

    if (showSplash) {
        PantallaSplash()
    } else {
        NavHost(navController = navController, startDestination = "mercadillos") {

            composable("logout") { PantallaLogoutSplash(navController) }

            composable("mercadillos") { PantallaMercadillos(navController) }

            composable("login") {
                PantallaLogin(onNavigateToMain = { navController.safePopBackStack() },
                    navController = navController
                    )
            }


            composable("splash_descarga") {
                PantallaSplashDescarga(
                    navController = navController,
                    destinoDespues = "mercadillos"
                )
            }

            composable("configuracion") { PantallaConfiguracion(navController) }
            composable("perfil") { PantallaPerfil(navController) }
            composable("categorias") { PantallaCategorias(navController) }
            composable("articulos") { PantallaArticulos(navController) }
            composable("inventario") { es.nuskysoftware.marketsales.ui.pantallas.PantallaInventario(navController) }
            composable("listados") { es.nuskysoftware.marketsales.ui.pantallas.PantallaListados(navController) }
            composable("utilidades") { PantallaUtilidades(navController) }
            composable("saldos_pendientes") { PantallaSaldosPendientes(navController) }
            composable("datos_empresa") { PantallaDatosEmpresa(navController) }

            // Pantalla de depuración para activar/desactivar datos (solo en compilaciones debug)
            if (BuildConfig.DEBUG) {
                composable("debug_datos") {
                    PantallaDebugDatos(navController)
                }
            }

            composable("alta_mercadillo") { PantallaAltaMercadillo(navController) }

            composable(
                route = "editar_mercadillo/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId")
                PantallaAltaMercadillo(navController, mercadilloId)
            }

            // Ventas
            composable(
                route = "ventas/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId")
                if (mercadilloId != null) {
                    PantallaVentasWrapper(navController, mercadilloId)
                }
            }

            // Gastos
            composable(
                route = "gastos/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable
                PantallaGastosWrapper(navController, mercadilloId)
            }

            // Método de pago
            composable(
                route = "metodo_pago/{mercadilloId}/{totalFmt}",
                arguments = listOf(
                    navArgument("mercadilloId") { type = NavType.StringType },
                    navArgument("totalFmt") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""

                PantallaMetodoPago(
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
                    onBack = { navController.safePopBackStack() }
                )
            }

            // Cambio (efectivo)
            composable(
                route = "cambio/{mercadilloId}/{totalFmt}",
                arguments = listOf(
                    navArgument("mercadilloId") { type = NavType.StringType },
                    navArgument("totalFmt") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
                PantallaCambio(
                    totalFormateado = totalFmt,
                    onBack = { navController.safePopBackStack() },
                    onConfirmarCambio = {
                        navController.navigate(
                            "enviar_recibo/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}/efectivo"
                        )
                    }
                )
            }

            // Carrito de gastos
            composable(
                route = "carrito_gastos/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val activity = LocalContext.current as ComponentActivity
                val gastosVM: es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel =
                    viewModel(activity, factory = es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory(activity.applicationContext))

                es.nuskysoftware.marketsales.ui.pantallas.gastos.PantallaGastosCarrito(
                    navController = navController,
                    gastosViewModel = gastosVM
                )
            }

            // Método de pago de gastos
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

                PantallaMetodoPago(
                    totalFormateado = totalFmt,
                    onMetodoSeleccionado = { metodo ->
                        val metodoStr = when (metodo) {
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.EFECTIVO -> "efectivo"
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.BIZUM -> "bizum"
                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.TARJETA -> "tarjeta"
                        }
                        gastosVM.cargarGastos(mercadilloId, metodoStr) {
                            navController.safePopBackStack(route = "mercadillos", inclusive = false)
                        }
                    },
                    onBack = { navController.safePopBackStack() }
                )
            }

            // Enviar recibo
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
                val ventasRoute = "ventas/$mercadilloId"
                val parent = navController.getBackStackEntry(ventasRoute)
                val context = LocalContext.current
                val ventasVM: es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel =
                    viewModel(parent, factory = es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModelFactory(context))

                PantallaEnviarRecibo(
                    totalFormateado = totalFmt,
                    metodo = metodo,
                    onBack = { navController.safePopBackStack() },
                    onEnviar = { via: TipoEnvio, destino: String ->
                        val ventasEntry = navController.getBackStackEntry(ventasRoute)
                        ventasEntry.savedStateHandle["finalizar_metodo"] = metodo
                        ventasEntry.savedStateHandle["enviar_via"] = via.name
                        ventasEntry.savedStateHandle["enviar_destino"] = destino
                        navController.safePopBackStack(route = ventasRoute, inclusive = false)
                    },
                    onFinalizarVenta = {
                        val metodoRepo = when (metodo.lowercase()) {
                            "efectivo" -> MetodoPagoRepo.EFECTIVO
                            "bizum" -> MetodoPagoRepo.BIZUM
                            else -> MetodoPagoRepo.TARJETA
                        }
                        ventasVM.finalizarVenta(metodoRepo)
                        navController.safePopBackStack(route = ventasRoute, inclusive = false)
                    }
                )
            }

            // Carrito
            composable(
                route = "carrito/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
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

            // Resumen
            composable(
                route = "resumen/{mercadilloId}",
                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable

                val cameFromArqueo = navController.previousBackStackEntry
                    ?.destination
                    ?.route
                    ?.startsWith("arqueo") == true

                PantallaResumenVentas(
                    mercadilloId = mercadilloId,
                    onBack = { navController.safePopBackStack() },
                    mostrarAbono = !cameFromArqueo
                )
            }

            // Subgrafo Arqueo
            addArqueoGraph(navController)
        }
    }
}

@Composable
fun PantallaVentasWrapper(navController: NavController, mercadilloId: String) {
    val context = LocalContext.current
    val mercadilloViewModel: MercadilloViewModel = viewModel(
        factory = MercadilloViewModelFactory(context)
    )

    LaunchedEffect(mercadilloId) { mercadilloViewModel.cargarMercadillo(mercadilloId) }

    val mercadilloParaEditar by mercadilloViewModel.mercadilloParaEditar.collectAsState()

    mercadilloParaEditar?.let { mercadillo ->
        PantallaVentas(
            navController = navController,
            mercadilloActivo = mercadillo
        )
    }
}

@Composable
fun PantallaGastosWrapper(navController: NavController, mercadilloId: String) {
    val context = LocalContext.current
    val mercadilloViewModel: MercadilloViewModel = viewModel(
        factory = MercadilloViewModelFactory(context)
    )

    LaunchedEffect(mercadilloId) { mercadilloViewModel.cargarMercadillo(mercadilloId) }

    val mercadilloParaEditar by mercadilloViewModel.mercadilloParaEditar.collectAsState()

    mercadilloParaEditar?.let { mercadillo ->
        es.nuskysoftware.marketsales.ui.pantallas.gastos.PantallaGastos(
            navController = navController,
            mercadilloActivo = mercadillo
        )
    }
}

private fun NavController.tryGetBackStackEntrySafe(route: String)
        : androidx.navigation.NavBackStackEntry? =
    try { getBackStackEntry(route) } catch (_: IllegalArgumentException) { null }

fun pingFirestoreProd() {
    val uid = Firebase.auth.currentUser?.uid ?: run {
        Log.w("MS-FB", "No hay usuario autenticado")
        return
    }
    val data = mapOf("uid" to uid, "when" to System.currentTimeMillis())
    Firebase.firestore.collection("healthchecks").document(uid)
        .set(data)
        .addOnSuccessListener { Log.d("MS-FB", "Ping Firestore PROD OK") }
        .addOnFailureListener { e -> Log.e("MS-FB", "Ping Firestore PROD FAIL", e) }
}



//// app/src/main/java/es/nuskysoftware/marketsales/MainActivity.kt
//package es.nuskysoftware.marketsales
//
//import android.annotation.SuppressLint
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import androidx.navigation.NavType
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.navigation.compose.rememberNavController
//import androidx.navigation.navArgument
//import es.nuskysoftware.marketsales.data.repository.ConfiguracionRepository
//import es.nuskysoftware.marketsales.ui.navigation.addArqueoGraph
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaAltaMercadillo
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaArticulos
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaCambio
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaCategorias
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaConfiguracion
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaDatosEmpresa
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaEnviarRecibo
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaLogin
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaMercadillos
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaMetodoPago
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaPerfil
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaResumenVentas
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaSaldosPendientes
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaSplash
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaSplashDescarga
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaLogoutSplash
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaUtilidades
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaVentas
//import es.nuskysoftware.marketsales.ui.pantallas.PantallaVentasCarrito
//import es.nuskysoftware.marketsales.ui.pantallas.TipoEnvio
//import es.nuskysoftware.marketsales.ui.theme.MarketSalesTheme
//import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModel
//import es.nuskysoftware.marketsales.ui.viewmodel.ConfiguracionViewModelFactory
//import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
//import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.data.repository.MetodoPago as MetodoPagoRepo
//import es.nuskysoftware.marketsales.utils.safePopBackStack
//import es.nuskysoftware.marketsales.ads.AdsConsentManager
//import com.google.android.gms.ads.MobileAds
//import com.google.firebase.Firebase
//import es.nuskysoftware.marketsales.ads.AdsInterstitialController   // ← mantiene import
//
//import com.google.firebase.auth.auth
//import com.google.firebase.firestore.firestore
//
//class MainActivity : ComponentActivity() {
//
//    companion object { private const val TAG = "MainActivity" }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        // 1) Inicializa el gestor UMP
//        AdsConsentManager.init(this)
//
//        Log.d(TAG, "🚀 MainActivity iniciada")
//        // 2) Pide/actualiza consentimiento y muestra el formulario si hace falta
//        AdsConsentManager.requestConsentAndShowFormIfRequired(this) {
//            // Se ejecuta cuando el formulario se cierra o no era necesario
//            MobileAds.initialize(this) {}
//            AdsInterstitialController.preload(this)   // precarga un interstitial
//        }
//
//        setContent {
//            val configuracionRepository = ConfiguracionRepository(this@MainActivity)
//            val configuracionViewModel: ConfiguracionViewModel =
//                viewModel(factory = ConfiguracionViewModelFactory(configuracionRepository))
//
//            MarketSalesTheme(configurationManager = ConfigurationManager) {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    NavigationSystem(configuracionViewModel)
//                }
//            }
//        }
//    }
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//@SuppressLint("UnrememberedGetBackStackEntry", "ContextCastToActivity")
//@Composable
//fun NavigationSystem(configuracionViewModel: ConfiguracionViewModel) {
//    val navController = rememberNavController()
//    var showSplash by remember { mutableStateOf(true) }
//
//    // ▼▼ Mostrar interstitial en cada cambio de pantalla ▼▼
//    val activity = LocalContext.current as? ComponentActivity
//    val canRequestAds by AdsConsentManager.canRequestAds.collectAsState()
//    val esPremium by ConfigurationManager.esPremium.collectAsState()
//    val backStackEntry by navController.currentBackStackEntryAsState()
//
//    LaunchedEffect(backStackEntry?.destination?.route, canRequestAds, esPremium) {
//        if (activity != null && canRequestAds && !esPremium) {
//            AdsInterstitialController.maybeShow(activity)
//        }
//    }
//    // ▲▲ Mostrar interstitial en cada cambio de pantalla ▲▲
//
//    LaunchedEffect(Unit) {
//        kotlinx.coroutines.delay(2000)
//        showSplash = false
//    }
//
//    if (showSplash) {
//        PantallaSplash()
//    } else {
//        NavHost(navController = navController, startDestination = "mercadillos") {
//
//            composable("logout") { PantallaLogoutSplash(navController) }
//
//            composable("mercadillos") { PantallaMercadillos(navController) }
//
//            composable("login") {
//                PantallaLogin(onNavigateToMain = { navController.safePopBackStack() })
//            }
//
//            composable("configuracion") { PantallaConfiguracion(navController) }
//            composable("perfil") { PantallaPerfil(navController) }
//            composable("categorias") { PantallaCategorias(navController) }
//            composable("articulos") { PantallaArticulos(navController) }
//            composable("inventario") { es.nuskysoftware.marketsales.ui.pantallas.PantallaInventario(navController) }
//            composable("listados") { es.nuskysoftware.marketsales.ui.pantallas.PantallaListados(navController) }
//            composable("utilidades") { PantallaUtilidades(navController) }
//            composable("saldos_pendientes") { PantallaSaldosPendientes(navController) }
//            composable("datos_empresa") { PantallaDatosEmpresa(navController) }
//
//            composable("alta_mercadillo") { PantallaAltaMercadillo(navController) }
//
//            composable(
//                route = "editar_mercadillo/{mercadilloId}",
//                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId")
//                PantallaAltaMercadillo(navController, mercadilloId)
//            }
//
//            // Ventas
//            composable(
//                route = "ventas/{mercadilloId}",
//                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId")
//                if (mercadilloId != null) {
//                    PantallaVentasWrapper(navController, mercadilloId)
//                }
//            }
//
//            // Gastos
//            composable(
//                route = "gastos/{mercadilloId}",
//                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable
//                PantallaGastosWrapper(navController, mercadilloId)
//            }
//
//            // Método de pago
//            composable(
//                route = "metodo_pago/{mercadilloId}/{totalFmt}",
//                arguments = listOf(
//                    navArgument("mercadilloId") { type = NavType.StringType },
//                    navArgument("totalFmt") { type = NavType.StringType }
//                )
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
//                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
//
//                PantallaMetodoPago(
//                    totalFormateado = totalFmt,
//                    onMetodoSeleccionado = { metodo ->
//                        when (metodo) {
//                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.EFECTIVO -> {
//                                navController.navigate(
//                                    "cambio/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}"
//                                )
//                            }
//                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.BIZUM -> {
//                                navController.navigate(
//                                    "enviar_recibo/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}/bizum"
//                                )
//                            }
//                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.TARJETA -> {
//                                navController.navigate(
//                                    "enviar_recibo/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}/tarjeta"
//                                )
//                            }
//                        }
//                    },
//                    onBack = { navController.safePopBackStack() }
//                )
//            }
//
//            // Cambio (efectivo)
//            composable(
//                route = "cambio/{mercadilloId}/{totalFmt}",
//                arguments = listOf(
//                    navArgument("mercadilloId") { type = NavType.StringType },
//                    navArgument("totalFmt") { type = NavType.StringType }
//                )
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
//                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
//                PantallaCambio(
//                    totalFormateado = totalFmt,
//                    onBack = { navController.safePopBackStack() },
//                    onConfirmarCambio = {
//                        navController.navigate(
//                            "enviar_recibo/${Uri.encode(mercadilloId)}/${Uri.encode(totalFmt)}/efectivo"
//                        )
//                    }
//                )
//            }
//
//            // Carrito de gastos
//            composable(
//                route = "carrito_gastos/{mercadilloId}",
//                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
//            ) { backStackEntry ->
//                val activity = LocalContext.current as ComponentActivity
//                val gastosVM: es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel =
//                    viewModel(activity, factory = es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory(activity.applicationContext))
//
//                es.nuskysoftware.marketsales.ui.pantallas.gastos.PantallaGastosCarrito(
//                    navController = navController,
//                    gastosViewModel = gastosVM
//                )
//            }
//
//            // Método de pago de gastos
//            composable(
//                route = "metodo_pago_gastos/{mercadilloId}/{totalFmt}",
//                arguments = listOf(
//                    navArgument("mercadilloId") { type = NavType.StringType },
//                    navArgument("totalFmt") { type = NavType.StringType }
//                )
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
//                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
//                val activity = LocalContext.current as ComponentActivity
//                val gastosVM: es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel =
//                    viewModel(activity, factory = es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory(activity.applicationContext))
//
//                PantallaMetodoPago(
//                    totalFormateado = totalFmt,
//                    onMetodoSeleccionado = { metodo ->
//                        val metodoStr = when (metodo) {
//                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.EFECTIVO -> "efectivo"
//                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.BIZUM -> "bizum"
//                            es.nuskysoftware.marketsales.ui.pantallas.MetodoPago.TARJETA -> "tarjeta"
//                        }
//                        gastosVM.cargarGastos(mercadilloId, metodoStr) {
//                            navController.safePopBackStack(route = "mercadillos", inclusive = false)
//                        }
//                    },
//                    onBack = { navController.safePopBackStack() }
//                )
//            }
//
//            // Enviar recibo
//            composable(
//                route = "enviar_recibo/{mercadilloId}/{totalFmt}/{metodo}",
//                arguments = listOf(
//                    navArgument("mercadilloId") { type = NavType.StringType },
//                    navArgument("totalFmt") { type = NavType.StringType },
//                    navArgument("metodo") { type = NavType.StringType }
//                )
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: ""
//                val totalFmt = backStackEntry.arguments?.getString("totalFmt") ?: ""
//                val metodo = backStackEntry.arguments?.getString("metodo") ?: ""
//                val ventasRoute = "ventas/$mercadilloId"
//                val parent = navController.getBackStackEntry(ventasRoute)
//                val context = LocalContext.current
//                val ventasVM: es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel =
//                    viewModel(parent, factory = es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModelFactory(context))
//
//                PantallaEnviarRecibo(
//                    totalFormateado = totalFmt,
//                    metodo = metodo,
//                    onBack = { navController.safePopBackStack() },
//                    onEnviar = { via: TipoEnvio, destino: String ->
//                        val ventasEntry = navController.getBackStackEntry(ventasRoute)
//                        ventasEntry.savedStateHandle["finalizar_metodo"] = metodo
//                        ventasEntry.savedStateHandle["enviar_via"] = via.name
//                        ventasEntry.savedStateHandle["enviar_destino"] = destino
//                        navController.safePopBackStack(route = ventasRoute, inclusive = false)
//                    },
//                    onFinalizarVenta = {
//                        val metodoRepo = when (metodo.lowercase()) {
//                            "efectivo" -> MetodoPagoRepo.EFECTIVO
//                            "bizum" -> MetodoPagoRepo.BIZUM
//                            else -> MetodoPagoRepo.TARJETA
//                        }
//                        ventasVM.finalizarVenta(metodoRepo)
//                        navController.safePopBackStack(route = ventasRoute, inclusive = false)
//                    }
//                )
//            }
//
//            // Carrito
//            composable(
//                route = "carrito/{mercadilloId}",
//                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable
//                val parent = navController.getBackStackEntry("ventas/$mercadilloId")
//                val context = LocalContext.current
//                val ventasVM: es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel =
//                    viewModel(parent, factory = es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModelFactory(context))
//
//                es.nuskysoftware.marketsales.ui.pantallas.PantallaVentasCarrito(
//                    navController = navController,
//                    ventasViewModel = ventasVM
//                )
//            }
//
//            // Resumen
//            composable(
//                route = "resumen/{mercadilloId}",
//                arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
//            ) { backStackEntry ->
//                val mercadilloId = backStackEntry.arguments?.getString("mercadilloId") ?: return@composable
//
//                val cameFromArqueo = navController.previousBackStackEntry
//                    ?.destination
//                    ?.route
//                    ?.startsWith("arqueo") == true
//
//                PantallaResumenVentas(
//                    mercadilloId = mercadilloId,
//                    onBack = { navController.safePopBackStack() },
//                    mostrarAbono = !cameFromArqueo
//                )
//            }
//
//            // Subgrafo Arqueo
//            addArqueoGraph(navController)
//        }
//    }
//}
//
//@Composable
//fun PantallaVentasWrapper(navController: NavController, mercadilloId: String) {
//    val context = LocalContext.current
//    val mercadilloViewModel: MercadilloViewModel = viewModel(
//        factory = MercadilloViewModelFactory(context)
//    )
//
//    LaunchedEffect(mercadilloId) { mercadilloViewModel.cargarMercadillo(mercadilloId) }
//
//    val mercadilloParaEditar by mercadilloViewModel.mercadilloParaEditar.collectAsState()
//
//    mercadilloParaEditar?.let { mercadillo ->
//        PantallaVentas(
//            navController = navController,
//            mercadilloActivo = mercadillo
//        )
//    }
//}
//
//@Composable
//fun PantallaGastosWrapper(navController: NavController, mercadilloId: String) {
//    val context = LocalContext.current
//    val mercadilloViewModel: MercadilloViewModel = viewModel(
//        factory = MercadilloViewModelFactory(context)
//    )
//
//    LaunchedEffect(mercadilloId) { mercadilloViewModel.cargarMercadillo(mercadilloId) }
//
//    val mercadilloParaEditar by mercadilloViewModel.mercadilloParaEditar.collectAsState()
//
//    mercadilloParaEditar?.let { mercadillo ->
//        es.nuskysoftware.marketsales.ui.pantallas.gastos.PantallaGastos(
//            navController = navController,
//            mercadilloActivo = mercadillo
//        )
//    }
//}
//
//private fun NavController.tryGetBackStackEntrySafe(route: String)
//        : androidx.navigation.NavBackStackEntry? =
//    try { getBackStackEntry(route) } catch (_: IllegalArgumentException) { null }
//
//fun pingFirestoreProd() {
//    val uid = Firebase.auth.currentUser?.uid ?: run {
//        Log.w("MS-FB", "No hay usuario autenticado")
//        return
//    }
//    val data = mapOf("uid" to uid, "when" to System.currentTimeMillis())
//    Firebase.firestore.collection("healthchecks").document(uid)
//        .set(data)
//        .addOnSuccessListener { Log.d("MS-FB", "Ping Firestore PROD OK") }
//        .addOnFailureListener { e -> Log.e("MS-FB", "Ping Firestore PROD FAIL", e) }
//}
//
//
