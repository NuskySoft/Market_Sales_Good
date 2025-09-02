// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaAltaMercadillo.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.ui.alta.*
import es.nuskysoftware.marketsales.data.repository.SaldoGuardadoRepository
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import es.nuskysoftware.marketsales.utils.safePopBackStack
import kotlinx.coroutines.launch
import es.nuskysoftware.marketsales.work.AutoEstadoScheduler
import androidx.compose.ui.platform.LocalContext
import es.nuskysoftware.marketsales.utils.StringResourceManager

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

    val saldoRepo = remember { SaldoGuardadoRepository(context) }
    var saldoPendiente by remember { mutableStateOf<SaldoGuardadoEntity?>(null) }
    var mostrarDialogoSaldoGuardado by remember { mutableStateOf(false) }

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
    var mostrarDialogoBorrado by remember { mutableStateOf(false) }

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

    // Mensajes / navegación
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            mercadilloViewModel.limpiarMensaje()
            try { AutoEstadoScheduler.runOnceNow(context.applicationContext) } catch (_: Exception) {}
            mercadilloViewModel.recargarDatos()
            navController.safePopBackStack()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) }
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
                title = {
                    Text(
                        text = if (esEdicion)
                            StringResourceManager.getString("alta_titulo_editar", currentLanguage).ifBlank { "Editar Mercadillo" }
                        else
                            StringResourceManager.getString("alta_titulo_nuevo", currentLanguage).ifBlank { "Nuevo Mercadillo" },
                        fontWeight = FontWeight.Bold
                    )
                },
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

                item {
                    CampoTexto(
                        valor = lugar,
                        onValueChange = { lugar = it },
                        label = StringResourceManager.getString("lugar", currentLanguage).ifBlank { "Lugar" },
                        placeholder = StringResourceManager.getString("placeholder_lugar", currentLanguage).ifBlank { "Ej: Plaza Mayor" }
                    )
                }

                item {
                    CampoTexto(
                        valor = organizador,
                        onValueChange = { organizador = it },
                        label = StringResourceManager.getString("organizador", currentLanguage).ifBlank { "Organizador" },
                        placeholder = StringResourceManager.getString("placeholder_organizador", currentLanguage).ifBlank { "Ej: Ayuntamiento" }
                    )
                }

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
                                StringResourceManager.getString("aviso_saldo_en_curso", currentLanguage)
                                    .ifBlank { "Aviso: cambiar el saldo inicial de un mercadillo EN CURSO puede provocar descuadre." },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (esEdicion && mercadilloParaEditar != null &&
                    (mercadilloParaEditar!!.estado == 1 || mercadilloParaEditar!!.estado == 2)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { mostrarDialogoBorrado = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                StringResourceManager.getString("borrar_mercadillo", currentLanguage)
                                    .ifBlank { "Borrar mercadillo" }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            // Botonera inferior
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (esEdicion) mercadilloViewModel.limpiarMercadilloParaEditar()
                        mercadilloViewModel.recargarDatos()
                        navController.safePopBackStack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(StringResourceManager.getString("cancelar", currentLanguage).ifBlank { "Cancelar" })
                }

                Button(
                    onClick = {
                        if (requiereConfirmacionSaldo) {
                            mostrarConfirmCambioSaldo = true
                        } else {
                            if (!esEdicion && ConfigurationManager.getIsPremium()) {
                                scope.launch {
                                    val guardado = saldoRepo.getUltimoNoConsumido()
                                    if (guardado != null) {
                                        saldoPendiente = guardado
                                        mostrarDialogoSaldoGuardado = true
                                    } else {
                                        guardar()
                                    }
                                }
                            } else {
                                guardar()
                            }
                        }
                    },
                    enabled = !uiState.loading && fecha.isNotBlank() && lugar.isNotBlank() && organizador.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            if (esEdicion)
                                StringResourceManager.getString("actualizar", currentLanguage).ifBlank { "Actualizar" }
                            else
                                StringResourceManager.getString("crear", currentLanguage).ifBlank { "Crear" }
                        )
                    }
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

        // Diálogo: saldo guardado pendiente
        if (mostrarDialogoSaldoGuardado && saldoPendiente != null) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoSaldoGuardado = false },
                title = {
                    Text(
                        StringResourceManager.getString("saldo_pendiente_titulo", currentLanguage)
                            .ifBlank { "Saldo pendiente" }
                    )
                },
                text = {
                    Text(
                        StringResourceManager.getString("saldo_pendiente_texto", currentLanguage)
                            .ifBlank { "Tienes un saldo guardado pendiente de asignar. ¿Quieres asignarlo a este mercadillo?" }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        mostrarDialogoSaldoGuardado = false
                        scope.launch {
                            saldoInicial = saldoPendiente!!.saldoInicialGuardado.toString()
                            guardar()
                            saldoRepo.borrarGuardado(saldoPendiente!!.idRegistro)
                            saldoPendiente = null
                        }
                    }) { Text(StringResourceManager.getString("si", currentLanguage).ifBlank { "Sí" }) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mostrarDialogoSaldoGuardado = false
                        guardar()
                        saldoPendiente = null
                    }) { Text(StringResourceManager.getString("no", currentLanguage).ifBlank { "No" }) }
                }
            )
        }

        // Diálogo: confirmar cambio de saldo (sin \n)
        if (mostrarConfirmCambioSaldo) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmCambioSaldo = false },
                title = {
                    Text(
                        StringResourceManager.getString("confirmar_cambio_saldo_titulo", currentLanguage)
                            .ifBlank { "Confirmar cambio de saldo" }
                    )
                },
                text = {
                    Column {
                        Text(
                            StringResourceManager.getString("confirmar_cambio_saldo_linea1", currentLanguage)
                                .ifBlank { "Has modificado el saldo inicial de un mercadillo EN CURSO. Esto puede provocar descuadre de caja." }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            StringResourceManager.getString("confirmar_cambio_saldo_linea2", currentLanguage)
                                .ifBlank { "¿Quieres guardar igualmente?" }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { mostrarConfirmCambioSaldo = false; guardar() }) {
                        Text(
                            StringResourceManager.getString("guardar_igualmente", currentLanguage)
                                .ifBlank { "Guardar igualmente" }
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarConfirmCambioSaldo = false }) {
                        Text(StringResourceManager.getString("cancelar", currentLanguage).ifBlank { "Cancelar" })
                    }
                }
            )
        }

        // Diálogo: borrar mercadillo
        if (mostrarDialogoBorrado) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoBorrado = false },
                title = {
                    Text(
                        StringResourceManager.getString("confirmar_borrado_titulo", currentLanguage)
                            .ifBlank { "Confirmar borrado" }
                    )
                },
                text = {
                    Text(
                        StringResourceManager.getString("confirmar_borrado_texto", currentLanguage)
                            .ifBlank { "¿Estás seguro de querer borrar el mercadillo?" }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        mostrarDialogoBorrado = false
                        mercadilloId?.let { mercadilloViewModel.borrarMercadillo(it) }
                    }) {
                        Text(StringResourceManager.getString("borrar", currentLanguage).ifBlank { "Borrar" })
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoBorrado = false }) {
                        Text(StringResourceManager.getString("cancelar", currentLanguage).ifBlank { "Cancelar" })
                    }
                }
            )
        }
    }
}
