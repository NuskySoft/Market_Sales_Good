package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.data.repository.SaldoGuardadoRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/* ===== Estado de UI (coincide con tu pantalla actual) ===== */

data class UIStateAsignarSaldo(
    val loading: Boolean = true,
    val mercadillos: List<MercadilloEntity> = emptyList(),   // destinos estados 1/2
    val mercadilloOrigen: MercadilloEntity? = null,
    val dialogoIntermedio: IntermediaState = IntermediaState(),
    val confirm: ConfirmState = ConfirmState(),
    val haySaldoGuardadoPrevio: Boolean = false              // para el segundo aviso
)

data class IntermediaState(
    val visible: Boolean = false,
    val modo: IntermediaModo? = null,
    val destino: MercadilloEntity? = null,
    val opcion: IntermediaOpcion = IntermediaOpcion.NINGUNA,
    val importeDigits: String = "" // centésimas
)

enum class IntermediaModo { GUARDAR, ASIGNAR }
enum class IntermediaOpcion { NINGUNA, RETIRAR, ANADIR }

data class ConfirmState(
    val visible: Boolean = false,
    val caso: ConfirmCaso? = null
)

enum class ConfirmCaso {
    GUARDAR_SIN_PROX,
    GUARDAR_CON_PROX,
    ASIGNAR_SIN_SALDO_DEST,
    ASIGNAR_CON_SALDO_DEST
}

data class TextoConfirm(
    val titulo: String,
    val lineas: List<LineaTexto>
)

data class LineaTexto(
    val texto: String,
    val esAvisoFinal: Boolean = false
)

/* ===== ViewModel ===== */

class AsignarSaldoViewModel(
    private val db: AppDatabase,
    private val mercadilloIdOrigen: String,
    private val mercRepo: MercadilloRepository,
    private val saldoRepo: SaldoGuardadoRepository
) : ViewModel() {

    private val _ui = androidx.compose.runtime.mutableStateOf(UIStateAsignarSaldo())
    val ui: androidx.compose.runtime.State<UIStateAsignarSaldo> get() = _ui

    private val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    init { cargarDatos() }

    private fun cargarDatos() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = db.mercadilloDao()
                val origen = dao.getMercadilloById(mercadilloIdOrigen)

                val userId = origen?.userId ?: ConfigurationManager.getCurrentUserId() ?: "usuario_default"
                val e1 = dao.getMercadillosByUserAndEstado(userId, 1).first()
                val e2 = dao.getMercadillosByUserAndEstado(userId, 2).first()

                val combinada = (e1 + e2)
                    .filter { it.idMercadillo != mercadilloIdOrigen }
                    .sortedWith(
                        compareBy<MercadilloEntity> { safeParseDate(it.fecha) }
                            .thenBy { it.horaInicio }
                    )

                val hayGuardadoPrevio = (saldoRepo.getUltimoNoConsumido() != null)

                _ui.value = UIStateAsignarSaldo(
                    loading = false,
                    mercadillos = combinada,
                    mercadilloOrigen = origen,
                    haySaldoGuardadoPrevio = hayGuardadoPrevio
                )
            } catch (_: Exception) {
                _ui.value = UIStateAsignarSaldo(loading = false)
            }
        }
    }

    private fun safeParseDate(fecha: String): Long =
        try { df.parse(fecha)?.time ?: Long.MAX_VALUE } catch (_: Exception) { Long.MAX_VALUE }

    /* ===== Formatos / cálculo ===== */

    /** Saldo final actual (o arqueo si aún no hay saldoFinal persistido). */
    private fun saldoFinalActual(): Double {
        val m = _ui.value.mercadilloOrigen ?: return 0.0
        return (m.saldoFinal ?: m.arqueoCaja ?: 0.0).coerceAtLeast(0.0)
    }

    fun saldoFinalActualFmt(): String = fmtMoneda(saldoFinalActual())

    /** Saldo final ajustado en vivo según opción + importe TPV. */
    private fun saldoFinalAjustado(): Double {
        val base = saldoFinalActual()
        val imp = importeActual()
        return when (_ui.value.dialogoIntermedio.opcion) {
            IntermediaOpcion.RETIRAR -> (base - imp).coerceAtLeast(0.0)
            IntermediaOpcion.ANADIR  -> (base + imp)
            IntermediaOpcion.NINGUNA -> base
        }
    }

    fun saldoFinalAjustadoFmt(): String = fmtMoneda(saldoFinalAjustado())

    fun fmtMoneda(valor: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale("es", "ES")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return nf.format(valor)
    }

    private fun importeActual(): Double {
        val digits = _ui.value.dialogoIntermedio.importeDigits
        if (digits.isBlank()) return 0.0
        val n = digits.toLongOrNull() ?: return 0.0
        return n / 100.0
    }

    /* ===== Acciones UI ===== */

    fun abrirIntermediaGuardar() {
        _ui.value = _ui.value.copy(
            dialogoIntermedio = IntermediaState(visible = true, modo = IntermediaModo.GUARDAR)
        )
    }

    fun abrirIntermediaAsignar(destino: MercadilloEntity) {
        _ui.value = _ui.value.copy(
            dialogoIntermedio = IntermediaState(visible = true, modo = IntermediaModo.ASIGNAR, destino = destino)
        )
    }

    fun cerrarIntermedia() {
        _ui.value = _ui.value.copy(dialogoIntermedio = IntermediaState()) // reset
    }

    fun seleccionarOpcion(opcion: IntermediaOpcion) {
        _ui.value = _ui.value.copy(
            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(opcion = opcion)
        )
    }

    fun onDigit(d: String) {
        val cur = _ui.value.dialogoIntermedio.importeDigits
        val nuevoRaw = (cur + d)
        val nuevo = if (nuevoRaw.isEmpty()) "" else nuevoRaw.trimStart('0')
        _ui.value = _ui.value.copy(
            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(importeDigits = nuevo)
        )
    }

    fun onDoubleZero() {
        val cur = _ui.value.dialogoIntermedio.importeDigits
        val nuevoRaw = cur + "00"
        val nuevo = if (nuevoRaw.isEmpty()) "" else nuevoRaw.trimStart('0')
        _ui.value = _ui.value.copy(
            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(importeDigits = nuevo)
        )
    }

    fun onClear() {
        val cur = _ui.value.dialogoIntermedio.importeDigits
        val nuevo = if (cur.isNotEmpty()) cur.dropLast(1) else ""
        _ui.value = _ui.value.copy(
            dialogoIntermedio = _ui.value.dialogoIntermedio.copy(importeDigits = nuevo)
        )
    }

    fun prepararConfirmacion() {
        val d = _ui.value.dialogoIntermedio
        val hayProximos = _ui.value.mercadillos.isNotEmpty()

        val caso = when (d.modo) {
            IntermediaModo.GUARDAR -> if (hayProximos) ConfirmCaso.GUARDAR_CON_PROX else ConfirmCaso.GUARDAR_SIN_PROX
            IntermediaModo.ASIGNAR -> {
                val saldoIni = d.destino?.saldoInicial ?: 0.0
                if (saldoIni == 0.0) ConfirmCaso.ASIGNAR_SIN_SALDO_DEST else ConfirmCaso.ASIGNAR_CON_SALDO_DEST
            }
            else -> return
        }

        _ui.value = _ui.value.copy(confirm = ConfirmState(visible = true, caso = caso))
    }

    fun cerrarConfirmacion() {
        _ui.value = _ui.value.copy(confirm = ConfirmState())
    }

    fun textoConfirmacion(): TextoConfirm {
        val caso = _ui.value.confirm.caso ?: return TextoConfirm("", emptyList())
        val hayPrevio = _ui.value.haySaldoGuardadoPrevio

        return when (caso) {
            ConfirmCaso.GUARDAR_SIN_PROX -> TextoConfirm(
                titulo = "Guardar saldo",
                lineas = buildList {
                    add(LineaTexto("¿Estás seguro de querer guardar el saldo?"))
                    add(LineaTexto("Se podrá utilizar como saldo inicial al dar de alta un mercadillo."))
                    if (hayPrevio) add(LineaTexto("Ya hay un saldo inicial guardado. ¿Seguro que deseas reemplazarlo? Esta operación no se puede deshacer.", esAvisoFinal = true))
                }
            )
            ConfirmCaso.GUARDAR_CON_PROX -> TextoConfirm(
                titulo = "Guardar saldo",
                lineas = buildList {
                    add(LineaTexto("¿Estás seguro de querer guardar el saldo?"))
                    add(LineaTexto("Si lo guardas, solo podrás asignarlo al crear un mercadillo nuevo."))
                    if (hayPrevio) add(LineaTexto("Ya hay un saldo inicial guardado. ¿Seguro que deseas reemplazarlo? Esta operación no se puede deshacer.", esAvisoFinal = true))
                }
            )
            ConfirmCaso.ASIGNAR_SIN_SALDO_DEST -> TextoConfirm(
                titulo = "Asignar saldo",
                lineas = listOf(LineaTexto("¿Estás seguro de querer asignar el saldo inicial a este mercadillo?"))
            )
            ConfirmCaso.ASIGNAR_CON_SALDO_DEST -> TextoConfirm(
                titulo = "Asignar saldo",
                lineas = listOf(
                    LineaTexto("El mercadillo seleccionado ya tiene saldo inicial. ¿Quieres reemplazar el saldo inicial?"),
                    LineaTexto("Esta operación no se puede deshacer.", esAvisoFinal = true)
                )
            )
        }
    }

    /** Persistencia de Paso 3. Llama desde el botón Aceptar del diálogo de confirmación. */
    fun confirmarYPersistir(navController: androidx.navigation.NavController) {
        val d = _ui.value.dialogoIntermedio
        if (d.modo == null) return

        val ajustado = saldoFinalAjustado()
        val origen = _ui.value.mercadilloOrigen ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (d.modo) {
                    IntermediaModo.GUARDAR -> {
                        // 1) Guardar/replace en tabla control
                        val item = SaldoGuardadoEntity(
                            idRegistro = UUID.randomUUID().toString(),
                            idUsuario = origen.userId ?: (ConfigurationManager.getCurrentUserId() ?: "usuario_default"),
                            idMercadilloOrigen = origen.idMercadillo,
                            fechaMercadillo = origen.fecha,
                            lugarMercadillo = origen.lugar,
                            organizadorMercadillo = origen.organizador,
                            horaInicioMercadillo = origen.horaInicio,
                            saldoInicialGuardado = ajustado,
                            consumido = false,
                            version = (origen.version ?: 0L) + 1L,
                            lastModified = System.currentTimeMillis(),
                            sincronizadoFirebase = false,
                            notas = null
                        )
                        saldoRepo.reemplazarGuardado(item)

                        // 2) Cerrar origen con saldoFinal ajustado (estado 6)
                        //    Reutilizamos repo.confirmarArqueoCaja para fijar saldo y estado.
                        mercRepo.confirmarArqueoCaja(
                            mercadilloId = origen.idMercadillo,
                            arqueoFinal = ajustado,
                            nuevoEstado = 6
                        )
                    }

                    IntermediaModo.ASIGNAR -> {
                        val destino = d.destino ?: return@launch
                        // 1) Asignar saldoInicial al destino (estado 2)
                        db.mercadilloDao().asignarSaldoInicial(destino.idMercadillo, ajustado)

                        // 2) Cerrar origen con saldoFinal ajustado (estado 6)
                        mercRepo.confirmarArqueoCaja(
                            mercadilloId = origen.idMercadillo,
                            arqueoFinal = ajustado,
                            nuevoEstado = 6
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    // limpiar diálogos y volver a home
                    _ui.value = _ui.value.copy(confirm = ConfirmState(), dialogoIntermedio = IntermediaState())
                    navController.navigate("mercadillos") {
                        popUpTo("mercadillos") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    // fall-back: cerrar modales para que el usuario pueda reintentar
                    _ui.value = _ui.value.copy(confirm = ConfirmState(), dialogoIntermedio = IntermediaState())
                }
            }
        }
    }

    companion object {
        fun factory(context: Context, mercadilloIdOrigen: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(context)
                    val mercRepo = MercadilloRepository(context)
                    val saldoRepo = SaldoGuardadoRepository(context)
                    return AsignarSaldoViewModel(db, mercadilloIdOrigen, mercRepo, saldoRepo) as T
                }
            }
    }
}
