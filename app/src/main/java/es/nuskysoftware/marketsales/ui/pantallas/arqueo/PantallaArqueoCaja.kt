// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaArqueoCaja.kt
package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import kotlin.math.abs

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

    val db = remember { AppDatabase.getDatabase(context) }
    var gastosEfectivo by remember { mutableStateOf(0.0) }

    var totalBilletesInput by remember { mutableStateOf("") }
    var totalMonedasInput by remember { mutableStateOf("") }

    LaunchedEffect(mercadilloId) {
        vm.cargar(mercadilloId)
        gastosEfectivo = withContext(Dispatchers.IO) {
            db.lineasGastosDao().getTotalGastosEfectivoDeCaja(mercadilloId)
        }
    }

    val scope = rememberCoroutineScope()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(StringResourceManager.getString("arqueo_caja", currentLanguage))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
                        )
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            ui.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ui.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            ui.mercadillo != null -> {
                val m = ui.mercadillo!!
                val resultadoEsperado = m.saldoInicial + m.ventasEfectivo - gastosEfectivo
                val totalBilletes = parseImporte(totalBilletesInput)
                val totalMonedas = parseImporte(totalMonedasInput)
                val totalContado = totalBilletes + totalMonedas
                val diferencia = totalContado - resultadoEsperado
                val puedeConfirmar = (m.estado == 4) && !guardando

                val estadoCajaTexto = when {
                    abs(diferencia) < 0.009 -> {
                        StringResourceManager.getString("caja_cuadra", currentLanguage)
                    }

                    diferencia > 0 -> {
                        "${StringResourceManager.getString("caja_descuadra_positivo", currentLanguage)}: ${vm.fmtMoneda(diferencia)}"
                    }

                    else -> {
                        "${StringResourceManager.getString("caja_descuadra_negativo", currentLanguage)}: ${vm.fmtMoneda(abs(diferencia))}"
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
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
                                value = vm.fmtMoneda(resultadoEsperado),
                                bold = true
                            )
                        }
                    }

                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = totalBilletesInput,
                                onValueChange = { totalBilletesInput = sanitizeImporteInput(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(StringResourceManager.getString("total_billetes", currentLanguage))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            OutlinedTextField(
                                value = totalMonedasInput,
                                onValueChange = { totalMonedasInput = sanitizeImporteInput(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(StringResourceManager.getString("total_monedas", currentLanguage))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            OutlinedTextField(
                                value = vm.fmtMoneda(totalContado),
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(StringResourceManager.getString("total_contado", currentLanguage))
                                },
                                singleLine = true,
                                readOnly = true
                            )

                            OutlinedTextField(
                                value = estadoCajaTexto,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(StringResourceManager.getString("estado_caja", currentLanguage))
                                },
                                singleLine = false,
                                minLines = 2,
                                readOnly = true
                            )
                        }
                    }

                    Button(
                        onClick = { mostrarDialogo = true },
                        enabled = puedeConfirmar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (guardando) {
                                StringResourceManager.getString("guardando", currentLanguage)
                            } else {
                                StringResourceManager.getString("confirmar_arqueo", currentLanguage)
                            }
                        )
                    }
                }

                if (mostrarDialogo) {
                    AlertDialog(
                        onDismissRequest = { mostrarDialogo = false },
                        title = {
                            Text(StringResourceManager.getString("confirmar_arqueo", currentLanguage))
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_unavez", currentLanguage))
                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_guardado", currentLanguage))
                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_premium", currentLanguage))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${StringResourceManager.getString("total_contado", currentLanguage)}: ${vm.fmtMoneda(totalContado)}"
                                )
                                Text(
                                    text = "${StringResourceManager.getString("estado_caja", currentLanguage)}: $estadoCajaTexto"
                                )
                                Spacer(modifier = Modifier.height(6.dp))
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
                                                saldoFinalCaja = totalContado
                                            )
                                            navController.safePopBackStack()
                                        } finally {
                                            guardando = false
                                        }
                                    }
                                }
                            ) {
                                Text(StringResourceManager.getString("confirmar", currentLanguage))
                            }
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

@Composable
private fun KeyValueLine(
    label: String,
    value: String,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else null
        )
    }
}

private fun sanitizeImporteInput(input: String): String {
    val filtered = input.filter { it.isDigit() || it == ',' || it == '.' }
    val normalized = filtered.replace('.', ',')
    val firstCommaIndex = normalized.indexOf(',')

    return if (firstCommaIndex >= 0) {
        val integerPart = normalized.substring(0, firstCommaIndex)
        val decimalPart = normalized
            .substring(firstCommaIndex + 1)
            .replace(",", "")
            .take(2)
        "$integerPart,$decimalPart"
    } else {
        normalized
    }
}

private fun parseImporte(value: String): Double {
    return value
        .replace(".", "")
        .replace(",", ".")
        .toDoubleOrNull()
        ?: 0.0
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
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.StringResourceManager
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
//    val currentLanguage by ConfigurationManager.idioma.collectAsState()
//
//    // Gastos en efectivo reales desde Room (solo los que descuentan de caja)
//    val db = remember { AppDatabase.getDatabase(context) }
//    var gastosEfectivo by remember { mutableStateOf(0.0) }
//
//    LaunchedEffect(mercadilloId) {
//        vm.cargar(mercadilloId)
//        gastosEfectivo = withContext(Dispatchers.IO) {
//            db.lineasGastosDao().getTotalGastosEfectivoDeCaja(mercadilloId)
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
//                title = { Text(StringResourceManager.getString("arqueo_caja", currentLanguage)) },
//                navigationIcon = {
//                    IconButton(onClick = { navController.safePopBackStack() }) {
//                        Icon(
//                            Icons.Default.ArrowBack,
//                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
//                        )
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
//                            KeyValueLine(
//                                label = StringResourceManager.getString("saldo_inicial", currentLanguage),
//                                value = vm.fmtMoneda(m.saldoInicial)
//                            )
//                            KeyValueLine(
//                                label = StringResourceManager.getString("ventas_en_efectivo", currentLanguage),
//                                value = vm.fmtMoneda(m.ventasEfectivo)
//                            )
//                            KeyValueLine(
//                                label = StringResourceManager.getString("gastos_en_efectivo", currentLanguage),
//                                value = vm.fmtMoneda(gastosEfectivo)
//                            )
//                            Divider()
//                            KeyValueLine(
//                                label = StringResourceManager.getString("resultado_arqueo", currentLanguage),
//                                value = vm.fmtMoneda(resultado),
//                                bold = true
//                            )
//                        }
//                    }
//                    Button(
//                        onClick = { mostrarDialogo = true },
//                        enabled = puedeConfirmar,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text(
//                            if (guardando)
//                                StringResourceManager.getString("guardando", currentLanguage)
//                            else
//                                StringResourceManager.getString("confirmar_arqueo", currentLanguage)
//                        )
//                    }
//                }
//
//                // Diálogo de confirmación
//                if (mostrarDialogo) {
//                    AlertDialog(
//                        onDismissRequest = { mostrarDialogo = false },
//                        title = { Text(StringResourceManager.getString("confirmar_arqueo", currentLanguage)) },
//                        text = {
//                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
//                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_unavez", currentLanguage))
//                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_guardado", currentLanguage))
//                                Text(StringResourceManager.getString("confirmar_arqueo_aviso_premium", currentLanguage))
//                                Spacer(Modifier.height(6.dp))
//                                Text(StringResourceManager.getString("confirmar_arqueo_pregunta", currentLanguage))
//                            }
//                        },
//                        confirmButton = {
//                            TextButton(
//                                onClick = {
//                                    mostrarDialogo = false
//                                    scope.launch {
//                                        guardando = true
//                                        try {
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
//                            ) { Text(StringResourceManager.getString("confirmar", currentLanguage)) }
//                        },
//                        dismissButton = {
//                            TextButton(onClick = { mostrarDialogo = false }) {
//                                Text(StringResourceManager.getString("cancelar", currentLanguage))
//                            }
//                        }
//                    )
//                }
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
//
//
