// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaArqueoCaja.kt
package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaArqueoCaja(
    navController: NavController,
    mercadilloId: String
) {
    val context = LocalContext.current
    val repo = remember { MercadilloRepository(context) }
    val vm: ArqueoViewModel = viewModel(factory = ArqueoViewModelFactory(context, repo))
    val ui by vm.ui.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    // Gastos en efectivo reales desde Room
    val db = remember { AppDatabase.getDatabase(context) }
    var gastosEfectivo by remember { mutableStateOf(0.0) }

    LaunchedEffect(mercadilloId) {
        vm.cargar(mercadilloId)
        gastosEfectivo = withContext(Dispatchers.IO) {
            db.lineasGastosDao().getTotalGastosPorMetodo(mercadilloId, "efectivo")
        }
    }

    val scope = rememberCoroutineScope()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResourceManager.getString("arqueo_caja", currentLanguage)) },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
                        )
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading -> Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { Text(ui.error!!, color = MaterialTheme.colorScheme.error) }

            ui.mercadillo != null -> {
                val m = ui.mercadillo!!
                val resultado = m.saldoInicial + m.ventasEfectivo - gastosEfectivo
                val puedeConfirmar = (m.estado == 4) && !guardando

                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KeyValueLine(
                                label = StringResourceManager.getString("saldo_inicial", currentLanguage),
                                value = vm.fmtMoneda(m.saldoInicial)
                            )
                            KeyValueLine(
                                label = StringResourceManager.getString("ventas_en_efectivo", currentLanguage),
                                value = vm.fmtMoneda(m.ventasEfectivo)
                            )
                            KeyValueLine(
                                label = StringResourceManager.getString("gastos_en_efectivo", currentLanguage),
                                value = vm.fmtMoneda(gastosEfectivo)
                            )
                            Divider()
                            KeyValueLine(
                                label = StringResourceManager.getString("resultado_arqueo", currentLanguage),
                                value = vm.fmtMoneda(resultado),
                                bold = true
                            )
                        }
                    }
                    Button(
                        onClick = { mostrarDialogo = true },
                        enabled = puedeConfirmar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (guardando)
                                StringResourceManager.getString("guardando", currentLanguage)
                            else
                                StringResourceManager.getString("confirmar_arqueo", currentLanguage)
                        )
                    }
                }

                // Diálogo de confirmación
                if (mostrarDialogo) {
                    AlertDialog(
                        onDismissRequest = { mostrarDialogo = false },
                        title = { Text(StringResourceManager.getString("confirmar_arqueo", currentLanguage)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_unavez", currentLanguage))
                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_guardado", currentLanguage))
                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_premium", currentLanguage))
                                Spacer(Modifier.height(6.dp))
                                Text(StringResourceManager.getString("confirmar_arqueo_pregunta", currentLanguage))
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    mostrarDialogo = false
                                    scope.launch {
                                        guardando = true
                                        try {
                                            vm.confirmarArqueoCaja(
                                                mercadilloId = mercadilloId,
                                                saldoFinalCaja = resultado
                                            )
                                            navController.safePopBackStack() // volver a PantallaArqueo
                                        } finally {
                                            guardando = false
                                        }
                                    }
                                }
                            ) { Text(StringResourceManager.getString("confirmar", currentLanguage)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { mostrarDialogo = false }) {
                                Text(StringResourceManager.getString("cancelar", currentLanguage))
                            }
                        }
                    )
                }
            }
        }
    }
}

/* ====== UI helper local ====== */

@Composable
private fun KeyValueLine(label: String, value: String, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else null
        )
    }
}


//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaArqueoCaja.kt
//package es.nuskysoftware.marketsales.ui.pantallas.arqueo
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
//import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModel
//import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModelFactory
//import es.nuskysoftware.marketsales.utils.safePopBackStack
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaArqueoCaja(
//    navController: NavController,
//    mercadilloId: String
//) {
//    val context = LocalContext.current
//    val repo = remember { MercadilloRepository(context) }
//    val vm: ArqueoViewModel = viewModel(factory = ArqueoViewModelFactory(context, repo))
//    val ui by vm.ui.collectAsState()
//
//    // Gastos en efectivo reales desde Room
//    val db = remember { AppDatabase.getDatabase(context) }
//    var gastosEfectivo by remember { mutableStateOf(0.0) }
//
//    LaunchedEffect(mercadilloId) {
//        vm.cargar(mercadilloId)
//        gastosEfectivo = withContext(Dispatchers.IO) {
//            db.lineasGastosDao().getTotalGastosPorMetodo(mercadilloId, "efectivo")
//        }
//    }
//
//    val scope = rememberCoroutineScope()
//    var mostrarDialogo by remember { mutableStateOf(false) }
//    var guardando by remember { mutableStateOf(false) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Arqueo de caja") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.safePopBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
//                    }
//                }
//            )
//        }
//    ) { pad ->
//        when {
//            ui.loading -> Box(
//                Modifier.fillMaxSize().padding(pad),
//                contentAlignment = Alignment.Center
//            ) { CircularProgressIndicator() }
//
//            ui.error != null -> Box(
//                Modifier.fillMaxSize().padding(pad),
//                contentAlignment = Alignment.Center
//            ) { Text(ui.error!!, color = MaterialTheme.colorScheme.error) }
//
//            ui.mercadillo != null -> {
//                val m = ui.mercadillo!!
//                val resultado = m.saldoInicial + m.ventasEfectivo - gastosEfectivo
//                val puedeConfirmar = (m.estado == 4) && !guardando
//
//                Column(
//                    Modifier
//                        .padding(pad)
//                        .fillMaxSize()
//                        .verticalScroll(rememberScrollState())
//                        .padding(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    Card {
//                        Column(
//                            Modifier.padding(16.dp),
//                            verticalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            KeyValueLine("Saldo inicial", vm.fmtMoneda(m.saldoInicial))
//                            KeyValueLine("Ventas en efectivo", vm.fmtMoneda(m.ventasEfectivo))
//                            KeyValueLine("Gastos en efectivo", vm.fmtMoneda(gastosEfectivo))
//                            Divider()
//                            KeyValueLine("Resultado del arqueo", vm.fmtMoneda(resultado), bold = true)
//                        }
//                    }
//                    Button(
//                        onClick = { mostrarDialogo = true },
//                        enabled = puedeConfirmar,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text(if (guardando) "Guardando…" else "Confirmar arqueo")
//                    }
//                }
//
//                // Diálogo de confirmación
//                if (mostrarDialogo) {
//                    AlertDialog(
//                        onDismissRequest = { mostrarDialogo = false },
//                        title = { Text("Confirmar arqueo") },
//                        text = {
//                            Text(
//                                "El arqueo solo se puede confirmar una vez.\n" +
//                                        "Se guardará el resultado del arqueo como saldo final de caja.\n" +
//                                        "Los usuarios Premium podrán asignar ese saldo a un nuevo mercadillo o guardarlo para más adelante.\n\n" +
//                                        "¿Estás seguro de querer confirmar el arqueo?"
//                            )
//                        },
//                        confirmButton = {
//                            TextButton(
//                                onClick = {
//                                    mostrarDialogo = false
//                                    scope.launch {
//                                        guardando = true
//                                        try {
//                                            // resultado = tu cálculo ya hecho (saldo inicial + ventas EF - gastos EF)
//                                            vm.confirmarArqueoCaja(
//                                                mercadilloId = mercadilloId,
//                                                saldoFinalCaja = resultado
//                                            )
//                                            navController.safePopBackStack() // volver a PantallaArqueo
//                                        } finally {
//                                            guardando = false
//                                        }
//                                    }
//                                }
//                            ) { Text("Confirmar") }
//                        },
//                        dismissButton = {
//                            TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
//                        }
//                    )
//                }
////                if (mostrarDialogo) {
////                    AlertDialog(
////                        onDismissRequest = { if (!guardando) mostrarDialogo = false },
////                        title = { Text("Confirmar arqueo") },
////                        text = {
////                            Text(
////                                "El arqueo solo se puede confirmar una vez.\n" +
////                                        "Se guardará el resultado del arqueo como saldo final de caja.\n" +
////                                        "Los usuarios Premium podrán asignar ese saldo a un nuevo mercadillo o guardarlo para más adelante.\n\n" +
////                                        "¿Quieres confirmar el arqueo?"
////                            )
////                        },
////                        confirmButton = {
////                            TextButton(
////                                enabled = !guardando,
////                                onClick = {
////                                    guardando = true
////                                    scope.launch {
////                                        try {
////                                            vm.confirmarArqueoCaja(
////                                                mercadilloId = m.id,
////                                                saldoFinalCaja = resultado
////                                            )
////                                            // Volver a PantallaArqueo
////                                            // Si tu graf usa ruta "arqueo/caja/$id", hacemos popBack.
////                                            // Si prefieres forzar recarga, puedes navegar a "arqueo/$id" con popUpTo.
////                                            navController.popBackStack()
////                                        } catch (e: Exception) {
////                                            // Mantenemos el diseño: solo cerramos diálogo y dejamos el botón habilitado
////                                        } finally {
////                                            guardando = false
////                                            mostrarDialogo = false
////                                        }
////                                    }
////                                }
////                            ) { Text("Sí, confirmar") }
////                        },
////                        dismissButton = {
////                            TextButton(
////                                enabled = !guardando,
////                                onClick = { mostrarDialogo = false }
////                            ) { Text("Cancelar") }
////                        }
////                    )
////                }
//            }
//        }
//    }
//}
//
///* ====== UI helper local ====== */
//
//@Composable
//private fun KeyValueLine(label: String, value: String, bold: Boolean = false) {
//    Row(
//        Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            label,
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//        Text(
//            value,
//            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
//            fontWeight = if (bold) FontWeight.SemiBold else null
//        )
//    }
//}
//
