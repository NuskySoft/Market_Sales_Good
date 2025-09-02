// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/MercadilloViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.MercadillosManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * MercadilloViewModel V16 - Market Sales
 *
 * Cambios clave:
 * - EN_CURSO: fecha NO editable (helper para la UI) y validación dura.
 * - EN_CURSO: cambio de saldo inicial requiere CONFIRMACIÓN previa (no se guarda hasta aceptar).
 * - UIState añade flags de confirmación + mensaje y ViewModel expone confirmar/cancelar.
 */
class MercadilloViewModel(
    private val repository: MercadilloRepository,
    appContext: Context
) : ViewModel() {

    companion object { private const val TAG = "MercadilloViewModel" }

    private val manager = MercadillosManager(appContext, repository)

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // ===== UI STATE =====
    private val _uiState = MutableStateFlow(MercadilloUiState())
    val uiState: StateFlow<MercadilloUiState> = _uiState.asStateFlow()

    // ===== EDICIÓN =====
    private val _mercadilloParaEditar = MutableStateFlow<MercadilloEntity?>(null)
    val mercadilloParaEditar: StateFlow<MercadilloEntity?> = _mercadilloParaEditar.asStateFlow()

    // Pendiente de confirmación (no exponer en UIState para no acoplar el entity)
    private var pendingUpdate: MercadilloEntity? = null

    // ===== MERCADILLO ACTIVO (BOTTOMBAR) =====
    private val _mercadilloActivoSeleccionado = MutableStateFlow<MercadilloEntity?>(null)
    val mercadilloActivoSeleccionado: StateFlow<MercadilloEntity?> = _mercadilloActivoSeleccionado.asStateFlow()

    // ===== FLOWS del manager =====
    val calendarioState = manager.calendarioState
    val nombreMesActual: StateFlow<String> = manager.nombreMesActual
    val mercadillosPorDia: StateFlow<Map<Int, List<MercadilloEntity>>> = manager.mercadillosPorDia
    val mercadillosEnCurso: StateFlow<List<MercadilloEntity>> = manager.mercadillosEnCurso
    val mercadillosProximos: StateFlow<List<MercadilloEntity>> = manager.mercadillosProximos
    val proximoMercadillo: StateFlow<MercadilloEntity?> =
        mercadillosProximos.map { it.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val tieneMercadillosProximos: StateFlow<Boolean> =
        mercadillosProximos.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val mostrarBottomBar: StateFlow<Boolean> = manager.mostrarBottomBar
    val tieneMercadillos: StateFlow<Boolean> = manager.tieneMercadillos

    val mercadillos: StateFlow<List<MercadilloEntity>> =
        repository.getMercadillosUsuarioActual()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mercadilloActivoParaOperaciones: StateFlow<MercadilloEntity?> = combine(
        mercadillosEnCurso, mercadilloActivoSeleccionado
    ) { enCurso, seleccionado ->
        when {
            enCurso.isEmpty() -> { _mercadilloActivoSeleccionado.value = null; null }
            enCurso.size == 1 -> {
                val unico = enCurso.first()
                if (seleccionado?.idMercadillo != unico.idMercadillo) {
                    _mercadilloActivoSeleccionado.value = unico
                }
                unico
            }
            seleccionado != null && enCurso.any { it.idMercadillo == seleccionado.idMercadillo } -> seleccionado
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { Log.d(TAG, "✅ MercadilloViewModel V16 inicializado") }

    // Acceso rápido a idioma y strings
    private val lang get() = ConfigurationManager.idioma.value
    private fun tr(key: String) = StringResourceManager.getString(key, lang)
    private fun tr1(key: String, arg0: String) = tr(key).replace("{0}", arg0)

    // ===== CRUD =====
    fun crearMercadillo(
        fecha: String,
        lugar: String,
        organizador: String,
        esGratis: Boolean = true,
        importeSuscripcion: Double = 0.0,
        requiereMesa: Boolean = true,
        requiereCarpa: Boolean = true,
        hayPuntoLuz: Boolean = false,
        horaInicio: String = "09:00",
        horaFin: String = "14:00",
        saldoInicial: Double? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                validarFechaParaAlta(fecha)?.let { fail(it); return@launch }
                validarMultiplesMercadillosPorDia(fecha)?.let { fail(it); return@launch }
                validarLugar(lugar)?.let { fail(it); return@launch }
                validarOrganizador(organizador)?.let { fail(it); return@launch }
                validarHorarios(horaInicio, horaFin)?.let { fail(it); return@launch }
                if (!esGratis) validarImporteSuscripcion(importeSuscripcion)?.let { fail(it); return@launch }
                if (saldoInicial != null) validarSaldoInicial(saldoInicial)?.let { fail(it); return@launch }

                val mercadilloId = repository.crearMercadillo(
                    fecha, lugar, organizador, esGratis, importeSuscripcion,
                    requiereMesa, requiereCarpa, hayPuntoLuz, horaInicio, horaFin, saldoInicial
                )

                val simbolo = (ConfigurationManager.moneda.value.split(" ").firstOrNull() ?: "€")
                val msg = if (saldoInicial != null) {
                    val importeFmt = "$simbolo ${String.format("%.2f", saldoInicial)}"
                    tr1("mercadillo_creado_con_saldo", importeFmt)
                } else {
                    tr("mercadillo_creado_ok")
                }

                _uiState.value = _uiState.value.copy(loading = false, message = msg, error = null)
                Log.d(TAG, "✅ Mercadillo creado: $lugar - $fecha (ID: $mercadilloId)")
                manager.refreshNow()
            } catch (e: Exception) {
                fail(tr1("error_creando_mercadillo", e.message ?: "—"), e)
            }
        }
    }

    fun cargarMercadillo(mercadilloId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val m = repository.getMercadilloById(mercadilloId)
                if (m != null) {
                    val puede = puedeEditarMercadillo(m)
                    if (!puede.first) { fail(puede.second); return@launch }
                    _mercadilloParaEditar.value = m
                } else {
                    fail(tr("mercadillo_no_encontrado")); return@launch
                }
            } catch (e: Exception) {
                fail(tr1("error_cargando_mercadillo", e.message ?: "—"), e)
            } finally { _uiState.value = _uiState.value.copy(loading = false) }
        }
    }

    fun actualizarMercadillo(
        mercadilloId: String,
        fecha: String,
        lugar: String,
        organizador: String,
        esGratis: Boolean = true,
        importeSuscripcion: Double = 0.0,
        requiereMesa: Boolean = true,
        requiereCarpa: Boolean = true,
        hayPuntoLuz: Boolean = false,
        horaInicio: String = "09:00",
        horaFin: String = "14:00",
        saldoInicial: Double? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val actual = repository.getMercadilloById(mercadilloId)
                    ?: run { fail(tr("mercadillo_no_encontrado")); return@launch }

                val puede = puedeEditarMercadillo(actual)
                if (!puede.first) { fail(puede.second); return@launch }

                // Reglas por estado (EN_CURSO: NO se puede cambiar la fecha)
                val errCampos = validarCamposSegunEstado(actual, fecha)
                if (errCampos != null) { fail(errCampos); return@launch }

                validarLugar(lugar)?.let { fail(it); return@launch }
                validarOrganizador(organizador)?.let { fail(it); return@launch }
                validarHorarios(horaInicio, horaFin)?.let { fail(it); return@launch }
                if (!esGratis) validarImporteSuscripcion(importeSuscripcion)?.let { fail(it); return@launch }
                if (saldoInicial != null) validarSaldoInicial(saldoInicial)?.let { fail(it); return@launch }

                val estadoActual = EstadosMercadillo.Estado.fromCodigo(actual.estado)
                val saldoCambioEnCurso = estadoActual == EstadosMercadillo.Estado.EN_CURSO &&
                        actual.saldoInicial != saldoInicial

                val actualizado = actual.copy(
                    fecha = fecha,
                    lugar = lugar,
                    organizador = organizador,
                    esGratis = esGratis,
                    importeSuscripcion = importeSuscripcion,
                    requiereMesa = requiereMesa,
                    requiereCarpa = requiereCarpa,
                    hayPuntoLuz = hayPuntoLuz,
                    horaInicio = horaInicio,
                    horaFin = horaFin,
                    saldoInicial = saldoInicial,
                    estado = calcularNuevoEstadoPorCambios(actual, saldoInicial)
                )

                if (saldoCambioEnCurso) {
                    // ❗ No guardes aún → pide confirmación
                    pendingUpdate = actualizado
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        pedirConfirmacionCambioSaldo = true,
                        textoConfirmacionCambioSaldo = tr("confirmar_cambio_saldo_en_curso")
                    )
                    return@launch
                }

                // Guardado normal (sin confirmación)
                if (repository.actualizarMercadillo(actualizado)) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = tr("mercadillo_actualizado_ok"),
                        error = null
                    )
                    _mercadilloParaEditar.value = null
                    Log.d(TAG, "✅ Mercadillo actualizado: $lugar - $fecha (ID: $mercadilloId)")
                    manager.refreshNow()
                } else {
                    fail(tr("error_actualizando_mercadillo"))
                }

            } catch (e: Exception) {
                fail(tr1("error_actualizando_mercadillo_detalle", e.message ?: "—"), e)
            }
        }
    }

    /** Confirmación del cambio de saldo en EN_CURSO (true=aceptar, false=cancelar). */
    fun confirmarCambioSaldoEnCurso(aceptar: Boolean) {
        viewModelScope.launch {
            val toApply = pendingUpdate
            // Cierra el diálogo
            _uiState.value = _uiState.value.copy(
                pedirConfirmacionCambioSaldo = false,
                textoConfirmacionCambioSaldo = null
            )
            if (!aceptar || toApply == null) {
                pendingUpdate = null
                return@launch
            }

            try {
                val ok = repository.actualizarMercadillo(toApply)
                if (ok) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = tr("mercadillo_actualizado_ok"),
                        error = null
                    )
                    _mercadilloParaEditar.value = null
                    manager.refreshNow()
                    Log.d(TAG, "✅ Actualizado tras confirmación de saldo en curso")
                } else {
                    fail(tr("error_actualizando_mercadillo"))
                }
            } catch (e: Exception) {
                fail(tr1("error_actualizando_mercadillo_detalle", e.message ?: "—"), e)
            } finally {
                pendingUpdate = null
            }
        }
    }

    fun limpiarMercadilloParaEditar() { _mercadilloParaEditar.value = null }

    fun borrarMercadillo(mercadilloId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val ok = repository.desactivarMercadillo(mercadilloId)
                if (ok) {
                    _uiState.value = _uiState.value.copy(loading = false, message = tr("mercadillo_eliminado"), error = null)
                    _mercadilloParaEditar.value = null
                    manager.refreshNow()
                    Log.d(TAG, "🗑️ Mercadillo desactivado: $mercadilloId")
                } else {
                    fail(tr("error_eliminando_mercadillo"))
                }
            } catch (e: Exception) {
                fail(tr1("error_eliminando_mercadillo_detalle", e.message ?: "—"), e)
            }
        }
    }

    // ===== MERCADILLO ACTIVO =====
    fun seleccionarMercadilloActivo(mercadillo: MercadilloEntity) {
        _mercadilloActivoSeleccionado.value = mercadillo
        Log.d(TAG, "✅ Mercadillo activo seleccionado: ${mercadillo.lugar}")
    }
    fun limpiarMercadilloActivo() { _mercadilloActivoSeleccionado.value = null }
    fun cambiarMercadilloActivo() { _mercadilloActivoSeleccionado.value = null }

    // ===== BottomBar navegación =====
    fun manejarNavegacionVentas(): Pair<Boolean, MercadilloEntity?> {
        val enCurso = mercadillosEnCurso.value
        val activo = mercadilloActivoParaOperaciones.value
        return when {
            enCurso.isEmpty() -> { _uiState.value = _uiState.value.copy(error = tr("no_hay_mercadillos_en_curso")); false to null }
            activo != null -> true to activo
            else -> false to null
        }
    }
    fun manejarNavegacionGastos(): Pair<Boolean, MercadilloEntity?> = manejarNavegacionVentas()
    fun manejarNavegacionResumen(): Pair<Boolean, MercadilloEntity?> = manejarNavegacionVentas()

    // ===== Consultas =====
    val mercadillosRequierenAtencion = repository.getMercadillosRequierenAtencion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val estadisticasPorEstado: StateFlow<Map<EstadosMercadillo.Estado, Int>> = flow {
        while (true) {
            emit(repository.getEstadisticasPorEstado())
            kotlinx.coroutines.delay(5 * 60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ===== Sync =====
    fun forzarSincronizacion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                repository.sincronizarSinEstadosAutomaticos()
                _uiState.value = _uiState.value.copy(loading = false, message = tr("sincronizacion_completada"), error = null)
                Log.d(TAG, "✅ Sincronización suave completada")
                manager.refreshNow()
            } catch (e: Exception) {
                fail(tr1("error_sincronizacion_detalle", e.message ?: "—"), e)
            }
        }
    }

    fun sincronizarSoloDesdeFirebase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val userId = ConfigurationManager.getCurrentUserId()
                if (userId != null) {
                    repository.sincronizarSinEstadosAutomaticos()
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = tr("sincronizacion_completada_sin_cambios"),
                        error = null
                    )
                    Log.d(TAG, "✅ Sincronización desde Firebase sin estados automáticos")
                    manager.refreshNow()
                } else {
                    fail(tr("usuario_no_autenticado"))
                }
            } catch (e: Exception) {
                fail(tr1("error_sincronizacion_detalle", e.message ?: "—"), e)
            }
        }
    }

    // ===== UI helpers =====
    fun limpiarMensajes() { _uiState.value = _uiState.value.copy(error = null, message = null) }
    fun limpiarError() { _uiState.value = _uiState.value.copy(error = null) }
    fun limpiarMensaje() { _uiState.value = _uiState.value.copy(message = null) }

    // ===== VALIDACIONES =====
    fun validarFecha(fecha: String): String? =
        when {
            fecha.isBlank() -> tr("valid_fecha_vacia")
            !fecha.matches(Regex("\\d{2}-\\d{2}-\\d{4}")) -> tr("valid_formato_fecha_invalido")
            else -> try { dateFormat.parse(fecha); null } catch (_: Exception) { tr("valid_fecha_invalida") }
        }

    fun validarFechaParaAlta(fecha: String): String? {
        validarFecha(fecha)?.let { return it }
        return try {
            val fm = dateFormat.parse(fecha) ?: return tr("valid_fecha_invalida")
            // ✅ Comparación SOLO por día (ignora horas): hoy permitido
            val hoyStr = dateFormat.format(Date())
            val hoyCero = dateFormat.parse(hoyStr)!!
            if (fm.before(hoyCero)) tr("valid_fecha_pasada") else null
        } catch (_: Exception) { tr("valid_fecha_invalida") }
    }

    private suspend fun validarMultiplesMercadillosPorDia(fecha: String): String? {
        return try {
            val userId = ConfigurationManager.getCurrentUserId() ?: return tr("usuario_no_autenticado")
            val esPremium = ConfigurationManager.getIsPremium()
            val existentes = repository.getMercadillosPorFecha(userId, fecha)
            if (existentes.isNotEmpty() && !esPremium)
                tr("free_limite_mercadillos_por_dia")
            else null
        } catch (e: Exception) {
            Log.e(TAG, "Error validando múltiples mercadillos", e)
            tr("valid_error_disponibilidad_fecha")
        }
    }

    fun validarLugar(lugar: String): String? =
        when {
            lugar.isBlank() -> tr("valid_lugar_vacio")
            lugar.length < 3 -> tr("valid_lugar_min")
            lugar.length > 100 -> tr("valid_lugar_max")
            else -> null
        }

    fun validarOrganizador(organizador: String): String? =
        when {
            organizador.isBlank() -> tr("valid_organizador_vacio")
            organizador.length < 3 -> tr("valid_organizador_min")
            organizador.length > 100 -> tr("valid_organizador_max")
            else -> null
        }

    /** Permite fin < inicio (horarios orientativos). */
    fun validarHorarios(horaInicio: String, horaFin: String): String? {
        return when {
            !horaInicio.matches(Regex("\\d{2}:\\d{2}")) -> tr("valid_hora_inicio_formato")
            !horaFin.matches(Regex("\\d{2}:\\d{2}")) -> tr("valid_hora_fin_formato")
            else -> {
                try { timeFormat.parse(horaInicio); timeFormat.parse(horaFin); null }
                catch (e: Exception) { tr("valid_horarios_invalidos") }
            }
        }
    }

    fun validarImporteSuscripcion(importe: Double): String? =
        when {
            importe < 0 -> tr("valid_importe_negativo")
            importe > 999_999.99 -> tr("valid_importe_alto")
            else -> null
        }

    fun validarSaldoInicial(saldo: Double): String? =
        when {
            saldo < 0 -> tr("valid_saldo_inicial_negativo")
            saldo > 999_999.99 -> tr("valid_saldo_inicial_alto")
            else -> null
        }

    fun validarSaldoFinal(saldo: Double): String? =
        when {
            saldo < 0 -> tr("valid_saldo_final_negativo")
            saldo > 999_999.99 -> tr("valid_saldo_final_alto")
            else -> null
        }

    // ===== UTILIDADES =====
    val totalMercadillos: StateFlow<Int> = mercadillos
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun getInformacionDia(dia: Int): List<EstadosMercadillo.Estado> {
        val delDia = mercadillosPorDia.value[dia] ?: emptyList()
        return delDia.map {
            EstadosMercadillo.Estado.fromCodigo(it.estado) ?: EstadosMercadillo.Estado.PROGRAMADO_PARCIAL
        }.sortedBy { EstadosMercadillo.obtenerPrioridad(it) }
    }

    fun puedeRecibirVentas(mercadillo: MercadilloEntity): Boolean {
        val estado = EstadosMercadillo.Estado.fromCodigo(mercadillo.estado)
            ?: EstadosMercadillo.Estado.PROGRAMADO_PARCIAL
        return EstadosMercadillo.puedeRecibirVentas(estado)
    }

    fun puedeSerCancelado(mercadillo: MercadilloEntity, tieneVentas: Boolean): Boolean {
        val estado = EstadosMercadillo.Estado.fromCodigo(mercadillo.estado)
            ?: EstadosMercadillo.Estado.PROGRAMADO_PARCIAL
        return EstadosMercadillo.puedeSerCancelado(estado, tieneVentas)
    }

    /** Helper para la UI: ¿la fecha es editable? EN_CURSO → false. */
    fun esFechaEditable(estadoCodigo: Int): Boolean {
        val est = EstadosMercadillo.Estado.fromCodigo(estadoCodigo) ?: return true
        return est != EstadosMercadillo.Estado.EN_CURSO
    }

    /** Recalcula/repinta al cambiar de usuario o volver de edición. */
    fun recargarDatos() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Recargando datos…")
            val userId = ConfigurationManager.getCurrentUserId()
            if (userId != null && userId != "usuario_default") {
                repository.actualizarEstadosAutomaticos(userId)
            }
            manager.refreshNow()
        }
    }

    fun navegarMesAnterior() = manager.navegarMesAnterior()
    fun navegarMesSiguiente() = manager.navegarMesSiguiente()

    // ===== DEBUG =====
    fun cambiarEstadoMercadillo(mercadilloId: String, nuevoEstado: EstadosMercadillo.Estado) {
        viewModelScope.launch {
            try {
                val m = repository.getMercadilloById(mercadilloId) ?: return@launch
                val actualizado = m.copy(
                    estado = nuevoEstado.codigo,
                    version = m.version + 1,
                    lastModified = System.currentTimeMillis(),
                    sincronizadoFirebase = false
                )
                val ok = repository.actualizarMercadillo(actualizado)
                if (ok) {
                    _uiState.value = _uiState.value.copy(message = tr1("estado_cambiado_a", nuevoEstado.descripcion))
                    Log.d(TAG, "🔧 DEBUG: Estado cambiado -> ${nuevoEstado.descripcion}")
                    manager.refreshNow()
                } else fail(tr("error_cambiando_estado"))
            } catch (e: Exception) {
                fail(tr1("error_cambiando_estado_detalle", e.message ?: "—"), e)
            }
        }
    }

    // ===== Internos =====
    private fun puedeEditarMercadillo(mercadillo: MercadilloEntity): Pair<Boolean, String> {
        val est = EstadosMercadillo.Estado.fromCodigo(mercadillo.estado)
            ?: return false to tr("estado_mercadillo_no_valido")
        return when (est) {
            EstadosMercadillo.Estado.PROGRAMADO_PARCIAL,
            EstadosMercadillo.Estado.PROGRAMADO_TOTAL -> true to ""
            EstadosMercadillo.Estado.EN_CURSO -> true to "" // restricciones se validan aparte
            EstadosMercadillo.Estado.PENDIENTE_ARQUEO,
            EstadosMercadillo.Estado.PENDIENTE_ASIGNAR_SALDO,
            EstadosMercadillo.Estado.CERRADO_COMPLETO,
            EstadosMercadillo.Estado.CANCELADO -> false to tr1("no_modificar_estado", est.descripcion)
        }
    }

    /** EN_CURSO: NO permite cambiar fecha. Saldo sí (con confirmación previa). */
    private fun validarCamposSegunEstado(
        mercadilloActual: MercadilloEntity,
        nuevaFecha: String
    ): String? {
        val est = EstadosMercadillo.Estado.fromCodigo(mercadilloActual.estado)
            ?: return tr("estado_mercadillo_no_valido")
        if (est == EstadosMercadillo.Estado.EN_CURSO && nuevaFecha != mercadilloActual.fecha) {
            return tr("no_modificar_fecha_en_curso")
        }
        return null
    }

    private fun calcularNuevoEstadoPorCambios(
        mercadilloActual: MercadilloEntity,
        nuevoSaldoInicial: Double?
    ): Int {
        val est = EstadosMercadillo.Estado.fromCodigo(mercadilloActual.estado) ?: return mercadilloActual.estado
        return when (est) {
            EstadosMercadillo.Estado.PROGRAMADO_PARCIAL ->
                if (nuevoSaldoInicial != null && mercadilloActual.saldoInicial == null)
                    EstadosMercadillo.Estado.PROGRAMADO_TOTAL.codigo else mercadilloActual.estado
            EstadosMercadillo.Estado.PROGRAMADO_TOTAL ->
                if (nuevoSaldoInicial == null && mercadilloActual.saldoInicial != null)
                    EstadosMercadillo.Estado.PROGRAMADO_PARCIAL.codigo else mercadilloActual.estado
            else -> mercadilloActual.estado
        }
    }

    private fun fail(msg: String, e: Exception? = null) {
        _uiState.value = _uiState.value.copy(loading = false, error = msg)
        if (e != null) Log.e(TAG, "❌ $msg", e) else Log.w(TAG, "⚠️ $msg")
    }
}

/** Estado de UI */
data class MercadilloUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,

    // Confirmación previa al cambio de saldo en EN_CURSO
    val pedirConfirmacionCambioSaldo: Boolean = false,
    val textoConfirmacionCambioSaldo: String? = null
)
