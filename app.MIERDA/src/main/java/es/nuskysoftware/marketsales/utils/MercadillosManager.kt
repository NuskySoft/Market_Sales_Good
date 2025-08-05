// app/src/main/java/es/nuskysoftware/marketsales/utils/MercadillosManager.kt
package es.nuskysoftware.marketsales.utils

import android.content.Context
import android.util.Log
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manager de estado para PantallaMercadillos.
 * - Lee SIEMPRE de Room (offline-first).
 * - Reacciona a cambios en tablas + “tick” temporal (cada minuto).
 * - Expone flows listos para usar en la UI.
 *
 * ⚠️ No hace “descarga masiva” ni toca AuthRepository.
 *    Para sync suave: repository.sincronizarSinEstadosAutomaticos() (opcional).
 */
class MercadillosManager(
    context: Context,
    private val repository: MercadilloRepository = MercadilloRepository(context)
) {
    // ===== Infra =====
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dfFecha = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val dfMesTitulo = SimpleDateFormat("LLLL yyyy", Locale.getDefault())

    // ===== Estado calendario =====
    data class CalendarioState(val ano: Int, val mes: Int) // mes: 1..12

    private val hoyCal = Calendar.getInstance()
    private val _calState = MutableStateFlow(
        CalendarioState(hoyCal.get(Calendar.YEAR), hoyCal.get(Calendar.MONTH) + 1)
    )
    val calendarioState: StateFlow<CalendarioState> = _calState.asStateFlow()

    fun navegarMesAnterior() = moveMonth(-1)
    fun navegarMesSiguiente() = moveMonth(+1)

    private fun moveMonth(delta: Int) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, _calState.value.ano)
            set(Calendar.MONTH, _calState.value.mes - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        _calState.value = CalendarioState(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    val nombreMesActual: StateFlow<String> =
        _calState.map { (y, m) ->
            Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }.time.let { dfMesTitulo.format(it) }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), dfMesTitulo.format(Date()))

    // ===== Tick 1/min para “en curso” =====
    private val minuteTicker: SharedFlow<Unit> = flow {
        emit(Unit)
        while (true) { delay(60_000); emit(Unit) }
    }.shareIn(scope, SharingStarted.Eagerly, replay = 1)

    /** Trigger manual (ej., al volver de edición) */
    private val _manualRefresh = MutableSharedFlow<Unit>(replay = 0)
    fun refreshNow() { scope.launchInIO { _manualRefresh.emit(Unit) } }

    // ===== Datos: mes visible =====
    private val mercadillosMes: Flow<List<MercadilloEntity>> =
        _calState
            .flatMapLatest { (ano, mes) -> repository.getMercadillosPorMes(ano, mes) }
            .combine(minuteTicker.onStart { emit(Unit) }) { lista, _ -> lista }
            .combine(_manualRefresh.onStart { emit(Unit) }) { lista, _ -> lista }
            .distinctUntilChanged()

    val mercadillosPorDia: StateFlow<Map<Int, List<MercadilloEntity>>> =
        mercadillosMes
            .map { lista -> lista.groupBy({ it.diaDelMes(dfFecha) }, { it }) }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ===== En curso (00:00 fecha -> 05:00 día siguiente, tz dispositivo) =====
    val mercadillosEnCurso: StateFlow<List<MercadilloEntity>> =
        repository.getMercadillosUsuarioActual()
            .combine(minuteTicker.onStart { emit(Unit) }) { lista, _ ->
                val now = System.currentTimeMillis()
                lista.filter { it.estaEnCurso(now, dfFecha) }
                    .sortedBy { safeParse(it.fecha, dfFecha) }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ===== Próximos (futuro estricto) =====
    val mercadillosProximos: StateFlow<List<MercadilloEntity>> =
        repository.getMercadillosUsuarioActual()
            .map { lista ->
                val hoy0 = truncToDay(System.currentTimeMillis())
                lista.filter { safeParse(it.fecha, dfFecha) > hoy0 }
                    .sortedBy { safeParse(it.fecha, dfFecha) }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ===== Flags auxiliares =====
    val tieneMercadillos: StateFlow<Boolean> =
        mercadillosMes.map { it.isNotEmpty() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    val mostrarBottomBar: StateFlow<Boolean> =
        mercadillosEnCurso.map { it.isNotEmpty() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    // ===== Utilidades =====
    private fun MercadilloEntity.diaDelMes(df: SimpleDateFormat): Int {
        val t = safeParse(this.fecha, df)
        return Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.DAY_OF_MONTH)
    }

    private fun truncToDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun safeParse(fecha: String, df: SimpleDateFormat): Long =
        try { df.parse(fecha)?.time ?: 0L }
        catch (e: Exception) { Log.w("MercadillosManager", "Fecha inválida: $fecha"); 0L }

    /** Regla “en curso”: [00:00 fecha, 05:00 día+1]; horas de mercadillo se ignoran. */
    private fun MercadilloEntity.estaEnCurso(now: Long, df: SimpleDateFormat): Boolean {
        val start = truncToDay(safeParse(this.fecha, df))
        val end = start + 29 * 60 * 60 * 1000L // 24h + 5h
        return now in start..end
    }

    // ===== Sync “suave” opcional (push pendientes + pull selectivo + merge) =====
    suspend fun syncSuave() {
        try { repository.sincronizarSinEstadosAutomaticos() }
        catch (_: Exception) { /* no bloquear UI */ }
    }

    // ---- helpers
    private fun CoroutineScope.launchInIO(block: suspend () -> Unit) =
        this.launch(Dispatchers.IO) { block() }
}
