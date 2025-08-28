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
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.ui.composables.gastos.PestanaGastosAutomaticas
import es.nuskysoftware.marketsales.ui.composables.gastos.PestanaGastosManual
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ContextCastToActivity")
@Composable
fun PantallaGastos(
    navController: NavController,
    mercadilloActivo: MercadilloEntity
) {
    val activity = LocalContext.current as ComponentActivity
    // ⬇️ ViewModel scopeado al Activity para compartir estado con carrito y método de pago
    val gastosViewModel: GastosViewModel =
        viewModel(activity, factory = GastosViewModelFactory(activity.applicationContext))

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nuevos Gastos - ${mercadilloActivo.lugar}",
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
                }
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
                    text = { Text("Ingresar Gasto") }
                )
//                Tab(
//                    selected = selectedTab == 1,
//                    onClick = { selectedTab = 1 },
//                    text = { Text("Automáticos") }
//                )
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
    }
}


//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/gastos/PantallaGastos.kt
//package es.nuskysoftware.marketsales.ui.pantallas.gastos
//
//import android.net.Uri
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.R
//import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
//import es.nuskysoftware.marketsales.ui.composables.gastos.PestanaGastosAutomaticas
//import es.nuskysoftware.marketsales.ui.composables.gastos.PestanaGastosManual
//import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
//import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModelFactory
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaGastos(
//    navController: NavController,
//    mercadilloActivo: MercadilloEntity
//) {
//    val context = androidx.compose.ui.platform.LocalContext.current
//    val gastosViewModel: GastosViewModel = viewModel(factory = GastosViewModelFactory(context))
//    var selectedTab by rememberSaveable { mutableStateOf(0) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = "Nuevos Gastos - ${mercadilloActivo.lugar}",
//                        fontWeight = FontWeight.Bold
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_arrow_left),
//                            contentDescription = "Atrás"
//                        )
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//        ) {
//            TabRow(selectedTabIndex = selectedTab) {
//                Tab(
//                    selected = selectedTab == 0,
//                    onClick = { selectedTab = 0 },
//                    text = { Text("Gasto Manual") }
//                )
//                Tab(
//                    selected = selectedTab == 1,
//                    onClick = { selectedTab = 1 },
//                    text = { Text("Automáticos") }
//                )
//            }
//
//            when (selectedTab) {
//                0 -> PestanaGastosManual(
//                    gastosViewModel = gastosViewModel,
//                    mercadilloActivo = mercadilloActivo,
//                    onAbrirCarrito = {
//                        navController.navigate("carrito_gastos/${mercadilloActivo.idMercadillo}")
//                    },
//                    onCargarGasto = { totalFmt ->
//                        navController.navigate(
//                            "metodo_pago_gastos/${mercadilloActivo.idMercadillo}/${Uri.encode(totalFmt)}"
//                        )
//                    }
//                )
//
//                1 -> PestanaGastosAutomaticas(
//                    gastosViewModel = gastosViewModel,
//                    mercadilloActivo = mercadilloActivo
//                )
//            }
//        }
//    }
//}


private fun fechaCorta(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val dd = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val mm = cal.get(java.util.Calendar.MONTH) + 1
    val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val mi = cal.get(java.util.Calendar.MINUTE)
    return "%02d/%02d %02d:%02d".format(dd, mm, hh, mi)
}

