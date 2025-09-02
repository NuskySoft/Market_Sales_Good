// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/gastos/PantallaGastos.kt
package es.nuskysoftware.marketsales.ui.pantallas.gastos

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.ui.composables.gastos.PestanaGastosAutomaticas
import es.nuskysoftware.marketsales.ui.composables.gastos.PestanaGastosManual
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ContextCastToActivity")
@Composable
fun PantallaGastos(
    navController: NavController,
    mercadilloActivo: MercadilloEntity
) {
    val activity = LocalContext.current as ComponentActivity
    val gastosViewModel: GastosViewModel =
        viewModel(activity, factory = GastosViewModelFactory(activity.applicationContext))

    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${mercadilloActivo.lugar} - " +
                                StringResourceManager.getString("gastos", currentLanguage),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = StringResourceManager.getString("volver", currentLanguage),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(StringResourceManager.getString("ingresar_gasto", currentLanguage)) }
                )
                // Si activas la pestaña de automáticos, internacionaliza también su etiqueta:
                // Tab(
                //     selected = selectedTab == 1,
                //     onClick = { selectedTab = 1 },
                //     text = { Text(StringResourceManager.getString("automaticos", currentLanguage)) }
                // )
            }

            when (selectedTab) {
                0 -> PestanaGastosManual(
                    gastosViewModel = gastosViewModel,
                    mercadilloActivo = mercadilloActivo,
                    onAbrirCarrito = {
                        navController.navigate("carrito_gastos/${mercadilloActivo.idMercadillo}")
                    },
                    onCargarGasto = { totalFmt ->
                        navController.navigate(
                            "metodo_pago_gastos/${mercadilloActivo.idMercadillo}/${Uri.encode(totalFmt)}"
                        )
                    }
                )
                1 -> PestanaGastosAutomaticas(
                    gastosViewModel = gastosViewModel,
                    mercadilloActivo = mercadilloActivo
                )
            }
        }
        AdsBottomBar()
    }
}

private fun fechaCorta(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val dd = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val mm = cal.get(java.util.Calendar.MONTH) + 1
    val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val mi = cal.get(java.util.Calendar.MINUTE)
    return "%02d/%02d %02d:%02d".format(dd, mm, hh, mi)
}
