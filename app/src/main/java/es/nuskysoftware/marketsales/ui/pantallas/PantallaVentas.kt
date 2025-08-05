// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaVentas.kt
package es.nuskysoftware.marketsales.ui.pantallas

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.PestanaVenta
import es.nuskysoftware.marketsales.data.repository.MetodoPago as MetodoPagoRepo
import es.nuskysoftware.marketsales.ui.composables.PestanaVentaManual
import es.nuskysoftware.marketsales.ui.composables.PestanaVentaProductos
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVentas(
    navController: NavController,
    mercadilloActivo: MercadilloEntity
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ventasViewModel: VentasViewModel = viewModel(
        factory = VentasViewModelFactory(context)
    )

    val uiState by ventasViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Inicializa VM
    LaunchedEffect(mercadilloActivo.idMercadillo, mercadilloActivo.userId) {
        ventasViewModel.inicializar(
            mercadilloId = mercadilloActivo.idMercadillo,
            usuarioId = mercadilloActivo.userId
        )
    }

    // Consume “finalizar venta” al volver del recibo
    val ventasRoute = remember(mercadilloActivo.idMercadillo) {
        "ventas/${mercadilloActivo.idMercadillo}"
    }
    val ventasBackEntry = remember(navController, ventasRoute) {
        navController.getBackStackEntry(ventasRoute)
    }
    val finalizarMetodoFlow =
        remember(ventasBackEntry) { ventasBackEntry.savedStateHandle.getStateFlow<String?>("finalizar_metodo", null) }

    LaunchedEffect(finalizarMetodoFlow) {
        finalizarMetodoFlow.filterNotNull().collectLatest { metodoStr ->
            val metodoRepo = when (metodoStr.lowercase()) {
                "efectivo" -> MetodoPagoRepo.EFECTIVO
                "bizum" -> MetodoPagoRepo.BIZUM
                else -> MetodoPagoRepo.TARJETA
            }
            ventasViewModel.finalizarVenta(metodoRepo)
            ventasBackEntry.savedStateHandle["finalizar_metodo"] = null
        }
    }

    // Errores
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); ventasViewModel.limpiarError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nueva Venta - ${mercadilloActivo.lugar}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Atrás"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = when (uiState.pestanaActiva) {
                    PestanaVenta.MANUAL -> 0
                    PestanaVenta.PRODUCTOS -> 1
                }
            ) {
                Tab(
                    selected = uiState.pestanaActiva == PestanaVenta.MANUAL,
                    onClick = { ventasViewModel.cambiarPestana(PestanaVenta.MANUAL) },
                    text = { Text("Venta Manual") }
                )
                Tab(
                    selected = uiState.pestanaActiva == PestanaVenta.PRODUCTOS,
                    onClick = { ventasViewModel.cambiarPestana(PestanaVenta.PRODUCTOS) },
                    text = { Text("Productos") },
                    enabled = true
                )
            }

            when (uiState.pestanaActiva) {
                PestanaVenta.MANUAL -> {
                    PestanaVentaManual(
                        ventasViewModel = ventasViewModel,
                        navController = navController,                  // 👈 ahora se pasa
                        mercadilloActivo = mercadilloActivo,            // 👈 ahora se pasa
                        onRealizarCargo = { totalFmt ->
                            navController.navigate(
                                "metodo_pago/${mercadilloActivo.idMercadillo}/${Uri.encode(totalFmt)}"
                            )
                        }
                    )
                }
                PestanaVenta.PRODUCTOS -> {
                    PestanaVentaProductos(
                        ventasViewModel = ventasViewModel,
                        navController = navController,                  // 👈 ahora se pasa
                        mercadilloActivo = mercadilloActivo,            // 👈 ahora se pasa
                        onRealizarCargo = { totalFmt ->
                            navController.navigate(
                                "metodo_pago/${mercadilloActivo.idMercadillo}/${Uri.encode(totalFmt)}"
                            )
                        }
                    )
                }
            }
        }
    }
}

