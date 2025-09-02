// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaResumenMercadillo.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.ui.composables.resumen.PestanaResumenGastos
import es.nuskysoftware.marketsales.ui.composables.resumen.PestanaResumenVentas
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaResumenVentas(
    mercadilloId: String,
    onBack: () -> Unit,
    mostrarAbono: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mercadilloVm: MercadilloViewModel = viewModel(factory = MercadilloViewModelFactory(context))
    LaunchedEffect(mercadilloId) { mercadilloVm.cargarMercadillo(mercadilloId) }
    val mercadillo by mercadilloVm.mercadilloParaEditar.collectAsState()

    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = mercadillo?.lugar
                            ?: StringResourceManager.getString("resumen", currentLanguage),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = StringResourceManager.getString("volver", currentLanguage),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(StringResourceManager.getString("ventas", currentLanguage)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(StringResourceManager.getString("gastos", currentLanguage)) }
                )
            }

            when (selectedTab) {
                0 -> PestanaResumenVentas(mercadilloId = mercadilloId, mostrarAbono = mostrarAbono)
                1 -> PestanaResumenGastos(mercadilloId = mercadilloId)
            }
        }
    }
    AdsBottomBar()
}
