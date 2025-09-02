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
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.PestanaVenta
import es.nuskysoftware.marketsales.ui.composables.PestanaVentaManual
import es.nuskysoftware.marketsales.ui.composables.PestanaVentaProductos
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModelFactory
import es.nuskysoftware.marketsales.utils.safePopBackStack
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

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
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    // Inicializa VM
    LaunchedEffect(mercadilloActivo.idMercadillo, mercadilloActivo.userId) {
        ventasViewModel.inicializar(
            mercadilloId = mercadilloActivo.idMercadillo,
            usuarioId = mercadilloActivo.userId
        )
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
                        text = "${StringResourceManager.getString("nueva_venta", currentLanguage)} - ${mercadilloActivo.lugar}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
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
                    text = { Text(StringResourceManager.getString("venta_manual", currentLanguage)) }
                )
                Tab(
                    selected = uiState.pestanaActiva == PestanaVenta.PRODUCTOS,
                    onClick = { ventasViewModel.cambiarPestana(PestanaVenta.PRODUCTOS) },
                    text = { Text(StringResourceManager.getString("productos", currentLanguage)) },
                    enabled = true
                )
            }

            when (uiState.pestanaActiva) {
                PestanaVenta.MANUAL -> {
                    PestanaVentaManual(
                        ventasViewModel = ventasViewModel,
                        navController = navController,
                        mercadilloActivo = mercadilloActivo,
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
                        navController = navController,
                        mercadilloActivo = mercadilloActivo,
                        onRealizarCargo = { totalFmt ->
                            navController.navigate(
                                "metodo_pago/${mercadilloActivo.idMercadillo}/${Uri.encode(totalFmt)}"
                            )
                        }
                    )
                }
            }
        }
        AdsBottomBar()
    }
}
