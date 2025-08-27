package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.ui.alta.*
import kotlinx.coroutines.launch
import es.nuskysoftware.marketsales.work.AutoEstadoScheduler   // ✅ IMPORTACIÓN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAltaMercadillo(
    navController: NavController,
    mercadilloId: String? = null,
    fechaPreseleccionada: String? = null
) {
    val context = LocalContext.current
    val mercadilloViewModel: MercadilloViewModel = viewModel(factory = MercadilloViewModelFactory(context))

    val uiState by mercadilloViewModel.uiState.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val mercadilloParaEditar by mercadilloViewModel.mercadilloParaEditar.collectAsState()

    val esEdicion = mercadilloId != null
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- Estado del formulario ---
    var fecha by remember { mutableStateOf(fechaPreseleccionada ?: "") }
    var lugar by remember { mutableStateOf("") }
    var organizador by remember { mutableStateOf("") }
    var esGratis by remember { mutableStateOf(true) }
    var importeSuscripcion by remember { mutableStateOf("") }
    var requiereMesa by remember { mutableStateOf(true) }
    var requiereCarpa by remember { mutableStateOf(true) }
    var hayPuntoLuz by remember { mutableStateOf(false) }
    var horaInicio by remember { mutableStateOf("09:00") }
    var horaFin by remember { mutableStateOf("14:00") }
    var saldoInicial by remember { mutableStateOf("") }

    var estadoDebugSeleccionado by remember { mutableStateOf<EstadosMercadillo.Estado?>(null) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarTimePickerInicio by remember { mutableStateOf(false) }
    var mostrarTimePickerFin by remember { mutableStateOf(false) }
    var mostrarConfirmCambioSaldo by remember { mutableStateOf(false) }

    // Cargar si edición
    LaunchedEffect(mercadilloId) { if (mercadilloId != null) mercadilloViewModel.cargarMercadillo(mercadilloId) }
    LaunchedEffect(mercadilloParaEditar) {
        mercadilloParaEditar?.let { m ->
            fecha = m.fecha; lugar = m.lugar; organizador = m.organizador
            esGratis = m.esGratis; importeSuscripcion = if (m.importeSuscripcion > 0) m.importeSuscripcion.toString() else ""
            requiereMesa = m.requiereMesa; requiereCarpa = m.requiereCarpa; hayPuntoLuz = m.hayPuntoLuz
            horaInicio = m.horaInicio; horaFin = m.horaFin
            saldoInicial = m.saldoInicial?.toString() ?: ""
            estadoDebugSeleccionado = EstadosMercadillo.Estado.fromCodigo(m.estado)
        }
    }
    LaunchedEffect(fechaPreseleccionada) {
        if (!fechaPreseleccionada.isNullOrBlank() && mercadilloViewModel.validarFecha(fechaPreseleccionada) == null) {
            fecha = fechaPreseleccionada
        }
    }

    // Mensajes
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            mercadilloViewModel.limpiarMensaje()
            // ✅ Tras crear/actualizar, disparamos catch-up de estados
            try { AutoEstadoScheduler.runOnceNow(context.applicationContext) } catch (_: Exception) {}
            mercadilloViewModel.recargarDatos()
            navController.popBackStack()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            mercadilloViewModel.limpiarError()
        }
    }

    // En curso → fecha bloqueada; confirmación si cambia saldo
    val estadoOriginalCodigo = mercadilloParaEditar?.estado
    val estadoFinalCodigo = estadoDebugSeleccionado?.codigo ?: estadoOriginalCodigo
    val enCursoFinal = estadoFinalCodigo == EstadosMercadillo.Estado.EN_CURSO.codigo

    val saldoOriginal = mercadilloParaEditar?.saldoInicial
    val saldoEditado = saldoInicial.toDoubleOrNull()
    val requiereConfirmacionSaldo = esEdicion && enCursoFinal && (
            (saldoOriginal ?: Double.NaN) != (saldoEditado ?: Double.NaN)
            )

    fun guardar() {
        val saldoInicialDouble = saldoInicial.toDoubleOrNull()
        val importeSuscripcionDouble = if (esGratis) 0.0 else (importeSuscripcion.toDoubleOrNull() ?: 0.0)

        scope.launch {
            try {
                if (esEdicion && mercadilloId != null) {
                    val estadoOriginal = mercadilloParaEditar?.estado
                    val estadoNuevo = estadoDebugSeleccionado?.codigo
                    if (estadoNuevo != null && estadoOriginal != null && estadoNuevo != estadoOriginal) {
                        mercadilloViewModel.cambiarEstadoMercadillo(mercadilloId, estadoDebugSeleccionado!!)
                    }
                    mercadilloViewModel.actualizarMercadillo(
                        mercadilloId, fecha, lugar, organizador, esGratis, importeSuscripcionDouble,
                        requiereMesa, requiereCarpa, hayPuntoLuz, horaInicio, horaFin, saldoInicialDouble
                    )
                } else {
                    mercadilloViewModel.crearMercadillo(
                        fecha, lugar, organizador, esGratis, importeSuscripcionDouble,
                        requiereMesa, requiereCarpa, hayPuntoLuz, horaInicio, horaFin, saldoInicialDouble
                    )
                }
            } catch (_: Exception) { /* VM ya setea el error */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Editar Mercadillo" else "Nuevo Mercadillo", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                item {
                    CampoFecha(
                        fecha = fecha,
                        onFechaChange = { fecha = it },
                        onMostrarDatePicker = { mostrarDatePicker = true },
                        enabled = !(esEdicion && enCursoFinal)
                    )
                }

                item { CampoTexto(valor = lugar, onValueChange = { lugar = it }, label = "Lugar", placeholder = "Ej: Plaza Mayor") }
                item { CampoTexto(valor = organizador, onValueChange = { organizador = it }, label = "Organizador", placeholder = "Ej: Ayuntamiento") }

                item {
                    ConfiguracionEconomica(
                        esGratis = esGratis,
                        onEsGratisChange = { esGratis = it },
                        importeSuscripcion = importeSuscripcion,
                        onImporteSuscripcionChange = { importeSuscripcion = it }
                    )
                }

                item {
                    ConfiguracionLogistica(
                        requiereMesa = requiereMesa, onRequiereMesaChange = { requiereMesa = it },
                        requiereCarpa = requiereCarpa, onRequiereCarpaChange = { requiereCarpa = it },
                        hayPuntoLuz = hayPuntoLuz, onHayPuntoLuzChange = { hayPuntoLuz = it }
                    )
                }

                item {
                    ConfiguracionHorarios(
                        horaInicio = horaInicio, onHoraInicioChange = { horaInicio = it },
                        horaFin = horaFin, onHoraFinChange = { horaFin = it },
                        onMostrarTimePickerInicio = { mostrarTimePickerInicio = true },
                        onMostrarTimePickerFin = { mostrarTimePickerFin = true }
                    )
                }

                item {
                    Column {
                        CampoSaldoInicial(saldoInicial = saldoInicial, onSaldoInicialChange = { saldoInicial = it })
                        if (requiereConfirmacionSaldo) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Aviso: cambiar el saldo inicial de un mercadillo EN CURSO puede provocar descuadre.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (esEdicion && mercadilloParaEditar != null
                    &&  (mercadilloParaEditar!!.estado == 1 || mercadilloParaEditar!!.estado == 2))


                {
                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { mercadilloId?.let { mercadilloViewModel.borrarMercadillo(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) { Text("Borrar mercadillo") }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            // Botonera
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (esEdicion) mercadilloViewModel.limpiarMercadilloParaEditar()
                        mercadilloViewModel.recargarDatos()
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) { Text("Cancelar") }

                Button(
                    onClick = { if (requiereConfirmacionSaldo) mostrarConfirmCambioSaldo = true else guardar() },
                    enabled = !uiState.loading && fecha.isNotBlank() && lugar.isNotBlank() && organizador.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else Text(if (esEdicion) "Actualizar" else "Crear")
                }
            }
        }

        // Pickers / diálogos
        if (mostrarDatePicker) {
            DatePickerDialog(
                onDateSelected = { fechaSeleccionada -> fecha = fechaSeleccionada; mostrarDatePicker = false },
                onDismiss = { mostrarDatePicker = false },
                currentLanguage = currentLanguage
            )
        }
        if (mostrarTimePickerInicio) {
            TimePickerDialog(
                onTimeSelected = { hora -> horaInicio = hora; mostrarTimePickerInicio = false },
                onDismiss = { mostrarTimePickerInicio = false },
                initialTime = horaInicio,
                currentLanguage = currentLanguage
            )
        }
        if (mostrarTimePickerFin) {
            TimePickerDialog(
                onTimeSelected = { hora -> horaFin = hora; mostrarTimePickerFin = false },
                onDismiss = { mostrarTimePickerFin = false },
                initialTime = horaFin,
                currentLanguage = currentLanguage
            )
        }
        if (mostrarConfirmCambioSaldo) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmCambioSaldo = false },
                title = { Text("Confirmar cambio de saldo") },
                text = { Text("Has modificado el saldo inicial de un mercadillo EN CURSO. Esto puede provocar descuadre de caja.\n\n¿Quieres guardar igualmente?") },
                confirmButton = {
                    TextButton(onClick = { mostrarConfirmCambioSaldo = false; guardar() }) { Text("Guardar igualmente") }
                },
                dismissButton = { TextButton(onClick = { mostrarConfirmCambioSaldo = false }) { Text("Cancelar") } }
            )
        }
    }
}

