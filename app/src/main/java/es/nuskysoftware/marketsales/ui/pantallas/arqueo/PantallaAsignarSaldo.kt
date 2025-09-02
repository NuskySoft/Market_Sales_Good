// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaAsignarSaldo.kt
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
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack
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
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    // VM EXTERNO (nuevo archivo)
    val vm: AsignarSaldoViewModel = viewModel(
        factory = AsignarSaldoViewModel.factory(context, mercadilloIdOrigen)
    )
    val ui by vm.ui

    val esPremium = remember { ConfigurationManager.isPremium() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResourceManager.getString("asignar_saldo", currentLanguage), fontWeight = FontWeight.Bold) },
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
        }
    ) { pad ->

        if (!esPremium) {
            Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    StringResourceManager.getString("opcion_solo_premium", currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navController.safePopBackStack() }) {
                    Text(StringResourceManager.getString("cerrar", currentLanguage))
                }
            }
            return@Scaffold
        }

        Column(Modifier.padding(pad).fillMaxSize()) {
            // Card "Guardar saldo"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { vm.abrirIntermediaGuardar() },
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            StringResourceManager.getString("guardar_saldo", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            StringResourceManager.getString("guardar_saldo_desc", currentLanguage),
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
                        currentLanguage = currentLanguage,
                        titulo = when (ui.dialogoIntermedio.modo) {
                            IntermediaModo.GUARDAR -> StringResourceManager.getString("guardar_saldo", currentLanguage)
                            IntermediaModo.ASIGNAR -> StringResourceManager.getString("asignar_saldo", currentLanguage)
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
                        onAceptar = { vm.prepararConfirmacion() }, // abre el AlertDialog
                        puedeAceptar = true,
                        leyendaImporte = when (ui.dialogoIntermedio.opcion) {
                            IntermediaOpcion.RETIRAR -> StringResourceManager.getString("importe_a_retirar", currentLanguage)
                            IntermediaOpcion.ANADIR -> StringResourceManager.getString("importe_a_anadir", currentLanguage)
                            else -> StringResourceManager.getString("selecciona_opcion_importe", currentLanguage)
                        },
                        mostrarTeclado = ui.dialogoIntermedio.opcion != IntermediaOpcion.NINGUNA,
                        fmt = { vm.fmtMoneda(it) }
                    )
                }
            }
        }

        // Diálogo de confirmación (texto generado por el VM)
        if (ui.confirm.visible) {
            val texto = remember(ui.confirm) { vm.textoConfirmacion() }
            AlertDialog(
                onDismissRequest = { vm.cerrarConfirmacion() },
                confirmButton = {
                    TextButton(onClick = {
                        vm.cerrarConfirmacion()
                        vm.confirmarYPersistir(navController)
                    }) { Text(StringResourceManager.getString("aceptar", currentLanguage)) }
                },
                dismissButton = {
                    TextButton(onClick = { vm.cerrarConfirmacion() }) {
                        Text(StringResourceManager.getString("cancelar", currentLanguage))
                    }
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
    currentLanguage: String,
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
            Text(
                StringResourceManager.getString("saldo_final", currentLanguage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(saldoFinalActualFmt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        // Display central: Saldo final AJUSTADO
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                saldoFinalAjustadoFmt,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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

        // Opciones (radios)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.RETIRAR) }
            ) {
                RadioButton(selected = opcion == IntermediaOpcion.RETIRAR, onClick = { onSeleccionOpcion(IntermediaOpcion.RETIRAR) })
                Spacer(Modifier.width(8.dp))
                Text(StringResourceManager.getString("retirar_efectivo", currentLanguage))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.ANADIR) }
            ) {
                RadioButton(selected = opcion == IntermediaOpcion.ANADIR, onClick = { onSeleccionOpcion(IntermediaOpcion.ANADIR) })
                Spacer(Modifier.width(8.dp))
                Text(StringResourceManager.getString("anadir_efectivo", currentLanguage))
            }
        }

        // Importe + Teclado (solo si hay opción)
        if (mostrarTeclado) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    leyendaImporte,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val impDouble = recordarImporteDesdeDigits(importeDigits)
                Text(
                    NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(impDouble),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
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
            OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
                Text(StringResourceManager.getString("cancelar", currentLanguage))
            }
            Button(onClick = onAceptar, modifier = Modifier.weight(1f), enabled = puedeAceptar) {
                Text(StringResourceManager.getString("aceptar", currentLanguage))
            }
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
//import android.content.Context
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
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.R
//import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
//import es.nuskysoftware.marketsales.ui.components.proximos.CardMercadillosProximos
//import es.nuskysoftware.marketsales.ui.composables.TecladoNumerico
//import es.nuskysoftware.marketsales.ui.viewmodel.*
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.StringResourceManager
//import es.nuskysoftware.marketsales.utils.safePopBackStack
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
//    val currentLanguage by ConfigurationManager.idioma.collectAsState()
//
//    // VM EXTERNO (nuevo archivo)
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
//                title = { Text(StringResourceManager.getString("asignar_saldo", currentLanguage), fontWeight = FontWeight.Bold) },
//                navigationIcon = {
//                    IconButton(onClick = { navController.safePopBackStack() }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_arrow_left),
//                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
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
//            Column(
//                Modifier.padding(pad).fillMaxSize().padding(16.dp),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    StringResourceManager.getString("opcion_solo_premium", currentLanguage),
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.SemiBold
//                )
//                Spacer(Modifier.height(12.dp))
//                Button(onClick = { navController.safePopBackStack() }) {
//                    Text(StringResourceManager.getString("cerrar", currentLanguage))
//                }
//            }
//            return@Scaffold
//        }
//
//        Column(Modifier.padding(pad).fillMaxSize()) {
//            // Card "Guardar saldo"
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
//                    Modifier.fillMaxWidth().padding(16.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column(Modifier.weight(1f)) {
//                        Text(
//                            StringResourceManager.getString("guardar_saldo", currentLanguage),
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Spacer(Modifier.height(4.dp))
//                        Text(
//                            StringResourceManager.getString("guardar_saldo_desc", currentLanguage),
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onPrimaryContainer,
//                            maxLines = 2,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                    Text("→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
//                }
//            }
//
//            // Próximos (estados 1 y 2)
//            val df = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
//
//            val proximos = remember(ui.mercadillos) {
//                ui.mercadillos.sortedWith(
//                    compareBy<MercadilloEntity>(
//                        { df.parse(it.fecha)?.time ?: Long.MAX_VALUE },
//                        { it.horaInicio }
//                    )
//                )
//            }
//
//            if (proximos.isNotEmpty()) {
//                CardMercadillosProximos(
//                    mercadillosProximos = proximos,
//                    onMercadilloClick = { vm.abrirIntermediaAsignar(it) }
//                )
//            }
//        }
//
//        // Diálogo intermedio
//        if (ui.dialogoIntermedio.visible) {
//            Dialog(onDismissRequest = { vm.cerrarIntermedia() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
//                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
//                    IntermediaContenido(
//                        titulo = when (ui.dialogoIntermedio.modo) {
//                            IntermediaModo.GUARDAR -> StringResourceManager.getString("guardar_saldo", currentLanguage)
//                            IntermediaModo.ASIGNAR -> StringResourceManager.getString("asignar_saldo", currentLanguage)
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
//                        onAceptar = { vm.prepararConfirmacion() }, // abre el AlertDialog
//                        puedeAceptar = true,
//                        leyendaImporte = when (ui.dialogoIntermedio.opcion) {
//                            IntermediaOpcion.RETIRAR -> StringResourceManager.getString("importe_a_retirar", currentLanguage)
//                            IntermediaOpcion.ANADIR -> StringResourceManager.getString("importe_a_anadir", currentLanguage)
//                            else -> StringResourceManager.getString("selecciona_opcion_importe", currentLanguage)
//                        },
//                        mostrarTeclado = ui.dialogoIntermedio.opcion != IntermediaOpcion.NINGUNA,
//                        fmt = { vm.fmtMoneda(it) }
//                    )
//                }
//            }
//        }
//
//        // Diálogo de confirmación (texto generado por el VM)
//        if (ui.confirm.visible) {
//            val texto = remember(ui.confirm) { vm.textoConfirmacion() }
//            AlertDialog(
//                onDismissRequest = { vm.cerrarConfirmacion() },
//                confirmButton = {
//                    TextButton(onClick = {
//                        vm.cerrarConfirmacion()
//                        vm.confirmarYPersistir(navController)
//                    }) { Text(StringResourceManager.getString("aceptar", currentLanguage)) }
//                },
//                dismissButton = {
//                    TextButton(onClick = { vm.cerrarConfirmacion() }) {
//                        Text(StringResourceManager.getString("cancelar", currentLanguage))
//                    }
//                },
//                title = { Text(texto.titulo) },
//                text = {
//                    Column {
//                        for (line in texto.lineas) {
//                            if (line.esAvisoFinal) {
//                                Text(
//                                    buildAnnotatedString {
//                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)) {
//                                            append(line.texto)
//                                        }
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
//        Modifier.fillMaxSize().padding(16.dp),
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
//            Text(
//                StringResourceManager.getString("saldo_final", currentLanguage),
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(saldoFinalActualFmt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
//        }
//
//        // Display central: Saldo final AJUSTADO
//        Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
//            Text(
//                saldoFinalAjustadoFmt,
//                style = MaterialTheme.typography.displaySmall,
//                fontWeight = FontWeight.Bold,
//                textAlign = TextAlign.Center
//            )
//            if (opcion != IntermediaOpcion.NINGUNA) {
//                val impDouble = recordarImporteDesdeDigits(importeDigits)
//                if (impDouble > 0.0) {
//                    val signo = if (opcion == IntermediaOpcion.RETIRAR) "− " else "+ "
//                    Text(
//                        "$signo${fmt(impDouble)}",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        textAlign = TextAlign.Center
//                    )
//                }
//            }
//        }
//
//        // Opciones (radios)
//        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.RETIRAR) }
//            ) {
//                RadioButton(selected = opcion == IntermediaOpcion.RETIRAR, onClick = { onSeleccionOpcion(IntermediaOpcion.RETIRAR) })
//                Spacer(Modifier.width(8.dp))
//                Text(StringResourceManager.getString("retirar_efectivo", ConfigurationManager.idioma.value))
//            }
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.clickable { onSeleccionOpcion(IntermediaOpcion.ANADIR) }
//            ) {
//                RadioButton(selected = opcion == IntermediaOpcion.ANADIR, onClick = { onSeleccionOpcion(IntermediaOpcion.ANADIR) })
//                Spacer(Modifier.width(8.dp))
//                Text(StringResourceManager.getString("anadir_efectivo", ConfigurationManager.idioma.value))
//            }
//        }
//
//        // Importe + Teclado (solo si hay opción)
//        if (mostrarTeclado) {
//            Column(
//                Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    leyendaImporte,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                val impDouble = recordarImporteDesdeDigits(importeDigits)
//                Text(
//                    NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(impDouble),
//                    style = MaterialTheme.typography.headlineLarge,
//                    fontWeight = FontWeight.SemiBold,
//                    textAlign = TextAlign.Center
//                )
//                Spacer(Modifier.height(8.dp))
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
//        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//            OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
//                Text(StringResourceManager.getString("cancelar", ConfigurationManager.idioma.value))
//            }
//            Button(onClick = onAceptar, modifier = Modifier.weight(1f), enabled = puedeAceptar) {
//                Text(StringResourceManager.getString("aceptar", ConfigurationManager.idioma.value))
//            }
//        }
//    }
//}
//
//@Composable
//private fun recordarImporteDesdeDigits(digits: String): Double {
//    if (digits.isBlank()) return 0.0
//    val n = digits.toLongOrNull() ?: return 0.0
//    return n / 100.0
//}
