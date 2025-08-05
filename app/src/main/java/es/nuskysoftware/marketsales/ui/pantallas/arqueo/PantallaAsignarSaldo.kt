package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity


import es.nuskysoftware.marketsales.ui.components.proximos.CardMercadillosProximos
import es.nuskysoftware.marketsales.ui.composables.TecladoNumerico
import es.nuskysoftware.marketsales.ui.viewmodel.*
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAsignarSaldo(
    navController: NavController,
    mercadilloIdOrigen: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // VM EXTERNO (nuevo archivo)
    val vm: AsignarSaldoViewModel = viewModel(
        factory = AsignarSaldoViewModel.factory(context, mercadilloIdOrigen)
    )
    val ui by vm.ui

    val esPremium = remember { ConfigurationManager.isPremium() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asignar saldo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_left), contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { pad ->

        if (!esPremium) {
            Column(
                Modifier.padding(pad).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Esta opción es solo para usuarios Premium.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Cerrar") }
            }
            return@Scaffold
        }

        Column(Modifier.padding(pad).fillMaxSize()) {
            // Card "Guarda Saldo" con estética Próximos
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { vm.abrirIntermediaGuardar() },
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Guarda Saldo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Conserva este saldo para asignarlo cuando crees un mercadillo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text("→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Próximos (estados 1 y 2)
// Próximos (estados 1 y 2)
            val df = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

            val proximos = remember(ui.mercadillos) {
                ui.mercadillos.sortedWith(
                    compareBy<MercadilloEntity>(
                        { df.parse(it.fecha)?.time ?: Long.MAX_VALUE },
                        { it.horaInicio }
                    )
                )
            }

            if (proximos.isNotEmpty()) {
                CardMercadillosProximos(
                    mercadillosProximos = proximos,
                    onMercadilloClick = { vm.abrirIntermediaAsignar(it) }
                )
            }
        }

        // Diálogo intermedio
        if (ui.dialogoIntermedio.visible) {
            Dialog(onDismissRequest = { vm.cerrarIntermedia() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    IntermediaContenido(
                        titulo = when (ui.dialogoIntermedio.modo) {
                            IntermediaModo.GUARDAR -> "Guardar saldo"
                            IntermediaModo.ASIGNAR -> "Asignar saldo"
                            else -> ""
                        },
                        saldoFinalActualFmt = vm.saldoFinalActualFmt(),
                        saldoFinalAjustadoFmt = vm.saldoFinalAjustadoFmt(),
                        importeDigits = ui.dialogoIntermedio.importeDigits,
                        opcion = ui.dialogoIntermedio.opcion,
                        onSeleccionOpcion = { vm.seleccionarOpcion(it) },
                        onDigit = { vm.onDigit(it) },
                        onDoubleZero = { vm.onDoubleZero() },
                        onClear = { vm.onClear() },
                        onCancelar = { vm.cerrarIntermedia() },
                        onAceptar = { vm.prepararConfirmacion() },         // solo abre el AlertDialog
                        puedeAceptar = true,                               // SIEMPRE habilitado
                        leyendaImporte = when (ui.dialogoIntermedio.opcion) {
                            IntermediaOpcion.RETIRAR -> "Importe a retirar"
                            IntermediaOpcion.ANADIR -> "Importe a añadir"
                            else -> "Selecciona una opción para introducir importe"
                        },
                        mostrarTeclado = ui.dialogoIntermedio.opcion != IntermediaOpcion.NINGUNA,
                        fmt = { vm.fmtMoneda(it) }
                    )
                }
            }
        }

        // Diálogo de confirmación (textos según caso)
        if (ui.confirm.visible) {
            val texto = remember(ui.confirm) { vm.textoConfirmacion() }
            AlertDialog(
                onDismissRequest = { vm.cerrarConfirmacion() },
                confirmButton = {
                    TextButton(onClick = {
                        vm.cerrarConfirmacion()
                        // 👉 Persistencia y navegación
                        vm.confirmarYPersistir(navController)
                        // El propio VM navega a "mercadillos" y cierra diálogos.
                    }) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { vm.cerrarConfirmacion() }) { Text("Cancelar") }
                },
                title = { Text(texto.titulo) },
                text = {
                    Column {
                        for (line in texto.lineas) {
                            if (line.esAvisoFinal) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)) {
                                            append(line.texto)
                                        }
                                    }
                                )
                            } else {
                                Text(line.texto)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            )
        }
    }
}

/* ================== UI intermedia (contenido del diálogo) ================== */

@Composable
private fun IntermediaContenido(
    titulo: String,
    saldoFinalActualFmt: String,
    saldoFinalAjustadoFmt: String,
    importeDigits: String,
    opcion: IntermediaOpcion,
    onSeleccionOpcion: (IntermediaOpcion) -> Unit,
    onDigit: (String) -> Unit,
    onDoubleZero: () -> Unit,
    onClear: () -> Unit,
    onCancelar: () -> Unit,
    onAceptar: () -> Unit,
    puedeAceptar: Boolean,
    leyendaImporte: String,
    mostrarTeclado: Boolean,
    fmt: (Double) -> String
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Divider()

        // Saldo final actual
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saldo final", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(saldoFinalActualFmt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        // Display central (centrado): Saldo final AJUSTADO
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                saldoFinalAjustadoFmt,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // Leyenda del importe tecleado con signo (si hay opción)
            if (opcion != IntermediaOpcion.NINGUNA) {
                val impDouble = recordarImporteDesdeDigits(importeDigits)
                if (impDouble > 0.0) {
                    val signo = if (opcion == IntermediaOpcion.RETIRAR) "− " else "+ "
                    Text(
                        "$signo${fmt(impDouble)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Radios
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.RETIRAR) }) {
                RadioButton(selected = opcion == IntermediaOpcion.RETIRAR, onClick = { onSeleccionOpcion(IntermediaOpcion.RETIRAR) })
                Spacer(Modifier.width(8.dp))
                Text("Retirar efectivo")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.ANADIR) }) {
                RadioButton(selected = opcion == IntermediaOpcion.ANADIR, onClick = { onSeleccionOpcion(IntermediaOpcion.ANADIR) })
                Spacer(Modifier.width(8.dp))
                Text("Añadir efectivo")
            }
        }

        // Importe + Teclado (solo si hay opción elegida)
        if (mostrarTeclado) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(leyendaImporte, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Valor centrado (renderiza lo que se va tecleando)
                val impDouble = recordarImporteDesdeDigits(importeDigits)
                Text(
                    NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(impDouble),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Tu teclado, sin inventar props
                TecladoNumerico(
                    onDigitClick = onDigit,
                    onClearClick = onClear,
                    onDoubleZeroClick = onDoubleZero
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Botones
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = onAceptar, modifier = Modifier.weight(1f), enabled = puedeAceptar) { Text("Aceptar") }
        }
    }
}

@Composable
private fun recordarImporteDesdeDigits(digits: String): Double {
    if (digits.isBlank()) return 0.0
    val n = digits.toLongOrNull() ?: return 0.0
    return n / 100.0
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaAsignarSaldo.kt
//package es.nuskysoftware.marketsales.ui.pantallas.arqueo
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.SpanStyle
//import androidx.compose.ui.text.buildAnnotatedString
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.text.withStyle
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.lifecycle.viewModelScope
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.R
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
//import es.nuskysoftware.marketsales.ui.components.proximos.CardMercadillosProximos
//import es.nuskysoftware.marketsales.ui.composables.TecladoNumerico
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//import java.text.NumberFormat
//import java.text.SimpleDateFormat
//import java.util.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaAsignarSaldo(
//    navController: NavController,
//    mercadilloIdOrigen: String
//) {
//    val context = androidx.compose.ui.platform.LocalContext.current
//    val vm: AsignarSaldoViewModel = viewModel(
//        factory = AsignarSaldoViewModel.factory(context, mercadilloIdOrigen)
//    )
//    val ui by vm.ui
//
//    val esPremium = remember { ConfigurationManager.isPremium() }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = "Asignar saldo",
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
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
//                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            )
//        }
//    ) { pad ->
//
//        if (!esPremium) {
//            // ===== Gate Premium =====
//            Column(
//                Modifier
//                    .padding(pad)
//                    .fillMaxSize()
//                    .padding(16.dp),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    "Esta opción es solo para usuarios Premium.",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.SemiBold
//                )
//                Spacer(Modifier.height(12.dp))
//                Button(onClick = { navController.popBackStack() }) {
//                    Text("Cerrar")
//                }
//            }
//            return@Scaffold
//        }
//
//        // ===== UI Premium =====
//        Column(
//            modifier = Modifier
//                .padding(pad)
//                .fillMaxSize()
//        ) {
//            // Card "Guarda Saldo" (misma estética que Próximos)
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
//                    .clickable { vm.abrirIntermediaGuardar() },
//                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
//                shape = MaterialTheme.shapes.medium
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column(Modifier.weight(1f)) {
//                        Text(
//                            "Guarda Saldo",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Spacer(Modifier.height(4.dp))
//                        Text(
//                            "Conserva este saldo para asignarlo cuando crees un mercadillo.",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onPrimaryContainer,
//                            maxLines = 2,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                    Text(
//                        "→",
//                        style = MaterialTheme.typography.titleMedium,
//                        color = MaterialTheme.colorScheme.onPrimaryContainer,
//                        modifier = Modifier.padding(start = 8.dp)
//                    )
//                }
//            }
//
//            // Debajo: Próximos (estados 1 y 2)
//            when {
//                ui.loading -> {
//                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                        CircularProgressIndicator()
//                    }
//                }
//                ui.mercadillos.isEmpty() -> {
//                    Text(
//                        "No tienes mercadillos programados",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier
//                            .padding(horizontal = 16.dp)
//                            .padding(top = 4.dp)
//                    )
//                }
//                else -> {
//                    CardMercadillosProximos(
//                        mercadillosProximos = ui.mercadillos,
//                        onMercadilloClick = { destino ->
//                            vm.abrirIntermediaAsignar(destino)
//                        }
//                    )
//                }
//            }
//        }
//
//        // ===== Diálogo a pantalla completa (intermedia) =====
//        if (ui.dialogoIntermedio.visible) {
//            Dialog(
//                onDismissRequest = { vm.cerrarIntermedia() },
//                properties = DialogProperties(usePlatformDefaultWidth = false)
//            ) {
//                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
//                    IntermediaContenido(
//                        titulo = when (ui.dialogoIntermedio.modo) {
//                            IntermediaModo.GUARDAR -> "Guardar saldo"
//                            IntermediaModo.ASIGNAR -> "Asignar saldo"
//                            else -> ""
//                        },
//                        saldoFinalActualFmt = vm.saldoFinalActualFmt(),
//                        saldoFinalAjustadoFmt = vm.saldoFinalAjustadoFmt(),
//                        importeDigits = ui.dialogoIntermedio.importeDigits,
//                        opcion = ui.dialogoIntermedio.opcion,
//                        onSeleccionOpcion = { vm.seleccionarOpcion(it) },
//                        onDigit = { vm.onDigit(it) },
//                        onDoubleZero = { vm.onDoubleZero() },
//                        onClear = { vm.onClear() },
//                        onCancelar = { vm.cerrarIntermedia() },
//                        onAceptar = { vm.prepararConfirmacion() },
//                        // ✅ Aceptar SIEMPRE habilitado
//                        puedeAceptar = true,
//                        // Leyenda contextual del importe tecleado
//                        leyendaImporte = when (ui.dialogoIntermedio.opcion) {
//                            IntermediaOpcion.RETIRAR -> "Importe a retirar"
//                            IntermediaOpcion.ANADIR -> "Importe a añadir"
//                            else -> "Selecciona una opción para introducir importe"
//                        },
//                        // ✅ Teclado solo si hay opción elegida
//                        mostrarTeclado = ui.dialogoIntermedio.opcion != IntermediaOpcion.NINGUNA,
//                        fmt = { vm.fmtMoneda(it) }
//                    )
//                }
//            }
//        }
//
//        // ===== Diálogo de confirmación (textos según caso) =====
//        if (ui.confirm.visible) {
//            val texto = remember(ui.confirm) { vm.textoConfirmacion() }
//            AlertDialog(
//                onDismissRequest = { vm.cerrarConfirmacion() },
//                confirmButton = {
//                    TextButton(onClick = {
//                        // Paso 3: persistencia (guardar o asignar) se implementará aquí.
//                        vm.cerrarConfirmacion()
//                        vm.cerrarIntermedia()
//                    }) { Text("Aceptar") }
//                },
//                dismissButton = {
//                    TextButton(onClick = { vm.cerrarConfirmacion() }) { Text("Cancelar") }
//                },
//                title = { Text(texto.titulo) },
//                text = {
//                    Column {
//                        for (line in texto.lineas) {
//                            if (line.esAvisoFinal) {
//                                Text(
//                                    buildAnnotatedString {
//                                        withStyle(
//                                            SpanStyle(
//                                                color = MaterialTheme.colorScheme.error,
//                                                fontWeight = FontWeight.Bold
//                                            )
//                                        ) { append(line.texto) }
//                                    }
//                                )
//                            } else {
//                                Text(line.texto)
//                            }
//                            Spacer(Modifier.height(4.dp))
//                        }
//                    }
//                }
//            )
//        }
//    }
//}
//
///* ================== UI intermedia (contenido del diálogo) ================== */
//
//@Composable
//private fun IntermediaContenido(
//    titulo: String,
//    saldoFinalActualFmt: String,
//    saldoFinalAjustadoFmt: String,
//    importeDigits: String,
//    opcion: IntermediaOpcion,
//    onSeleccionOpcion: (IntermediaOpcion) -> Unit,
//    onDigit: (String) -> Unit,
//    onDoubleZero: () -> Unit,
//    onClear: () -> Unit,
//    onCancelar: () -> Unit,
//    onAceptar: () -> Unit,
//    puedeAceptar: Boolean,
//    leyendaImporte: String,
//    mostrarTeclado: Boolean,
//    fmt: (Double) -> String
//) {
//    Column(
//        Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        // Encabezado
//        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
//        Divider()
//
//        // Saldo final actual
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text("Saldo final", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
//            Text(saldoFinalActualFmt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
//        }
//
//        // ===== Display central (centrado): Saldo final AJUSTADO =====
//        Column(
//            Modifier
//                .fillMaxWidth()
//                .padding(top = 4.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                saldoFinalAjustadoFmt,
//                style = MaterialTheme.typography.displaySmall,
//                fontWeight = FontWeight.Bold,
//                textAlign = TextAlign.Center
//            )
//            // Leyenda del importe tecleado con signo (si hay opción)
//            if (opcion != IntermediaOpcion.NINGUNA) {
//                val importeDouble = recordarImporteDesdeDigits(importeDigits)
//                if (importeDouble > 0.0) {
//                    val signo = if (opcion == IntermediaOpcion.RETIRAR) "− " else "+ "
//                    Text(
//                        "$signo${fmt(importeDouble)}",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        textAlign = TextAlign.Center
//                    )
//                }
//            }
//        }
//
//        // Radios
//        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.RETIRAR) }
//            ) {
//                RadioButton(
//                    selected = opcion == IntermediaOpcion.RETIRAR,
//                    onClick = { onSeleccionOpcion(IntermediaOpcion.RETIRAR) }
//                )
//                Spacer(Modifier.width(8.dp))
//                Text("Retirar efectivo")
//            }
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.ANADIR) }
//            ) {
//                RadioButton(
//                    selected = opcion == IntermediaOpcion.ANADIR,
//                    onClick = { onSeleccionOpcion(IntermediaOpcion.ANADIR) }
//                )
//                Spacer(Modifier.width(8.dp))
//                Text("Añadir efectivo")
//            }
//        }
//
//        // Importe + Teclado (solo si hay opción elegida)
//        if (mostrarTeclado) {
//            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                Text(leyendaImporte, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                TecladoNumerico(
//                    onDigitClick = onDigit,
//                    onClearClick = onClear,
//                    onDoubleZeroClick = onDoubleZero
//                )
//            }
//        }
//
//        Spacer(Modifier.weight(1f))
//
//        // Botones
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            OutlinedButton(
//                onClick = onCancelar,
//                modifier = Modifier.weight(1f)
//            ) { Text("Cancelar") }
//            Button(
//                onClick = onAceptar,
//                modifier = Modifier.weight(1f),
//                enabled = puedeAceptar // ✅ siempre true (según requisito)
//            ) { Text("Aceptar") }
//        }
//    }
//}
//
//@Composable
//private fun recordarImporteDesdeDigits(digits: String): Double {
//    // "1234" -> 12.34
//    if (digits.isBlank()) return 0.0
//    val n = digits.toLongOrNull() ?: return 0.0
//    return n / 100.0
//}
//
///* ================== ViewModel y estado (Paso 2: solo flujos UI) ================== */
//
//data class UIState(
//    val loading: Boolean = true,
//    val mercadillos: List<MercadilloEntity> = emptyList(),
//    val mercadilloOrigen: MercadilloEntity? = null,
//    val dialogoIntermedio: IntermediaState = IntermediaState(),
//    val confirm: ConfirmState = ConfirmState()
//)
//
//data class IntermediaState(
//    val visible: Boolean = false,
//    val modo: IntermediaModo? = null,
//    val destino: MercadilloEntity? = null,
//    val opcion: IntermediaOpcion = IntermediaOpcion.NINGUNA,
//    val importeDigits: String = "" // se representa en centésimas
//)
//
//enum class IntermediaModo { GUARDAR, ASIGNAR }
//enum class IntermediaOpcion { NINGUNA, RETIRAR, ANADIR }
//
//data class ConfirmState(
//    val visible: Boolean = false,
//    val caso: ConfirmCaso? = null
//)
//
//enum class ConfirmCaso {
//    GUARDAR_SIN_PROX,
//    GUARDAR_CON_PROX,
//    ASIGNAR_SIN_SALDO_DEST,
//    ASIGNAR_CON_SALDO_DEST
//}
//
//data class TextoConfirm(
//    val titulo: String,
//    val lineas: List<LineaTexto>
//)
//
//data class LineaTexto(
//    val texto: String,
//    val esAvisoFinal: Boolean = false
//)
//
//class AsignarSaldoViewModel(
//    private val appDb: AppDatabase,
//    private val mercadilloIdOrigen: String
//) : ViewModel() {
//
//    private val _ui = mutableStateOf(UIState())
//    val ui: State<UIState> get() = _ui
//
//    private val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
//
//    init {
//        cargarDatos()
//    }
//
//    private fun cargarDatos() {
//        _ui.value = _ui.value.copy(loading = true)
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val dao = appDb.mercadilloDao()
//                val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
//
//                val origen = dao.getMercadilloById(mercadilloIdOrigen)
//                val e1 = dao.getMercadillosByUserAndEstado(userId, 1).first()
//                val e2 = dao.getMercadillosByUserAndEstado(userId, 2).first()
//
//                val combinada = (e1 + e2)
//                    .filter { it.idMercadillo != mercadilloIdOrigen } // no listar el origen
//                    .sortedWith(
//                        compareBy<MercadilloEntity> { safeParseDate(it.fecha) }
//                            .thenBy { it.horaInicio }
//                    )
//
//                _ui.value = UIState(
//                    loading = false,
//                    mercadillos = combinada,
//                    mercadilloOrigen = origen
//                )
//            } catch (_: Exception) {
//                _ui.value = UIState(loading = false)
//            }
//        }
//    }
//
//    private fun safeParseDate(fecha: String): Long {
//        return try { df.parse(fecha)?.time ?: Long.MAX_VALUE } catch (_: Exception) { Long.MAX_VALUE }
//    }
//
//    /* ===== Formatos / cálculo ===== */
//
//    /** Saldo final actual (o arqueo si aún no hay saldoFinal persistido). */
//    private fun saldoFinalActual(): Double {
//        val m = _ui.value.mercadilloOrigen ?: return 0.0
//        return (m.saldoFinal ?: m.arqueoCaja ?: 0.0).coerceAtLeast(0.0)
//    }
//
//    fun saldoFinalActualFmt(): String = fmtMoneda(saldoFinalActual())
//
//    /** Saldo final ajustado en vivo según opción + importe TPV. */
//    private fun saldoFinalAjustado(): Double {
//        val base = saldoFinalActual()
//        val imp = importeActual()
//        return when (_ui.value.dialogoIntermedio.opcion) {
//            IntermediaOpcion.RETIRAR -> (base - imp).coerceAtLeast(0.0)
//            IntermediaOpcion.ANADIR -> (base + imp)
//            IntermediaOpcion.NINGUNA -> base
//        }
//    }
//
//    fun saldoFinalAjustadoFmt(): String = fmtMoneda(saldoFinalAjustado())
//
//    fun fmtMoneda(valor: Double): String {
//        val nf = NumberFormat.getNumberInstance(Locale("es", "ES")).apply {
//            minimumFractionDigits = 2
//            maximumFractionDigits = 2
//        }
//        return nf.format(valor)
//    }
//
//    private fun importeActual(): Double {
//        val digits = _ui.value.dialogoIntermedio.importeDigits
//        if (digits.isBlank()) return 0.0
//        val n = digits.toLongOrNull() ?: return 0.0
//        return n / 100.0
//    }
//
//    /* ===== Acciones UI ===== */
//
//    fun abrirIntermediaGuardar() {
//        _ui.value = _ui.value.copy(
//            dialogoIntermedio = IntermediaState(visible = true, modo = IntermediaModo.GUARDAR)
//        )
//    }
//
//    fun abrirIntermediaAsignar(destino: MercadilloEntity) {
//        _ui.value = _ui.value.copy(
//            dialogoIntermedio = IntermediaState(visible = true, modo = IntermediaModo.ASIGNAR, destino = destino)
//        )
//    }
//
//    fun cerrarIntermedia() {
//        _ui.value = _ui.value.copy(dialogoIntermedio = IntermediaState()) // reset
//    }
//
//    fun seleccionarOpcion(opcion: IntermediaOpcion) {
//        _ui.value = _ui.value.copy(
//            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(opcion = opcion)
//        )
//    }
//
//    fun onDigit(d: String) {
//        val cur = _ui.value.dialogoIntermedio.importeDigits
//        val nuevoRaw = (cur + d)
//        val nuevo = if (nuevoRaw.isEmpty()) "" else nuevoRaw.trimStart('0')
//        _ui.value = _ui.value.copy(
//            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(importeDigits = nuevo)
//        )
//    }
//
//    fun onDoubleZero() {
//        val cur = _ui.value.dialogoIntermedio.importeDigits
//        val nuevoRaw = cur + "00"
//        val nuevo = if (nuevoRaw.isEmpty()) "" else nuevoRaw.trimStart('0')
//        _ui.value = _ui.value.copy(
//            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(importeDigits = nuevo)
//        )
//    }
//
//    fun onClear() {
//        val cur = _ui.value.dialogoIntermedio.importeDigits
//        val nuevo = if (cur.isNotEmpty()) cur.dropLast(1) else ""
//        _ui.value = _ui.value.copy(
//            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(importeDigits = nuevo)
//        )
//    }
//
//    fun prepararConfirmacion() {
//        val d = _ui.value.dialogoIntermedio
//        val hayProximos = _ui.value.mercadillos.isNotEmpty()
//
//        val caso = when (d.modo) {
//            IntermediaModo.GUARDAR -> if (hayProximos) ConfirmCaso.GUARDAR_CON_PROX else ConfirmCaso.GUARDAR_SIN_PROX
//            IntermediaModo.ASIGNAR -> {
//                val saldoIni = d.destino?.saldoInicial ?: 0.0
//                if (saldoIni == 0.0) ConfirmCaso.ASIGNAR_SIN_SALDO_DEST else ConfirmCaso.ASIGNAR_CON_SALDO_DEST
//            }
//            else -> return
//        }
//
//        _ui.value = _ui.value.copy(confirm = ConfirmState(visible = true, caso = caso))
//    }
//
//    fun cerrarConfirmacion() {
//        _ui.value = _ui.value.copy(confirm = ConfirmState())
//    }
//
//    fun textoConfirmacion(): TextoConfirm {
//        return when (_ui.value.confirm.caso) {
//            ConfirmCaso.GUARDAR_SIN_PROX -> TextoConfirm(
//                titulo = "Guardar saldo",
//                lineas = listOf(
//                    LineaTexto("¿Estás seguro de querer guardar el saldo?"),
//                    LineaTexto("Se podrá utilizar como saldo inicial al dar de alta un mercadillo.")
//                    // Paso 3: si existe saldo guardado previo, aquí añadiremos el segundo aviso.
//                )
//            )
//            ConfirmCaso.GUARDAR_CON_PROX -> TextoConfirm(
//                titulo = "Guardar saldo",
//                lineas = listOf(
//                    LineaTexto("¿Estás seguro de querer guardar el saldo?"),
//                    LineaTexto("Si lo guardas, solo podrás asignarlo al crear un mercadillo nuevo.")
//                    // Paso 3: si existe saldo guardado previo, aquí añadiremos el segundo aviso.
//                )
//            )
//            ConfirmCaso.ASIGNAR_SIN_SALDO_DEST -> TextoConfirm(
//                titulo = "Asignar saldo",
//                lineas = listOf(
//                    LineaTexto("¿Estás seguro de querer asignar el saldo inicial a este mercadillo?")
//                )
//            )
//            ConfirmCaso.ASIGNAR_CON_SALDO_DEST -> TextoConfirm(
//                titulo = "Asignar saldo",
//                lineas = listOf(
//                    LineaTexto("El mercadillo seleccionado ya tiene saldo inicial. ¿Quieres reemplazar el saldo inicial?"),
//                    LineaTexto("Esta operación no se puede deshacer.", esAvisoFinal = true)
//                )
//            )
//            else -> TextoConfirm("", emptyList())
//        }
//    }
//
//    companion object {
//        fun factory(context: android.content.Context, mercadilloIdOrigen: String): ViewModelProvider.Factory =
//            object : ViewModelProvider.Factory {
//                @Suppress("UNCHECKED_CAST")
//                override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                    return AsignarSaldoViewModel(
//                        appDb = AppDatabase.getDatabase(context),
//                        mercadilloIdOrigen = mercadilloIdOrigen
//                    ) as T
//                }
//            }
//    }
//}
//
//
