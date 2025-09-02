// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/ArqueoViewModel.kt
/**
 * ViewModel de Arqueo:
 * - Carga el mercadillo y ventas por método desde Repository.
 * - Expone UiState para Arqueo/Resultado/AsignarSaldo.
 * - Añadido: confirmarArqueoCaja(...) que fija saldoFinal + arqueoCaja y cambia el estado (5 Premium, 6 Free).
 */
package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class ArqueoViewModel(
    private val repository: MercadilloRepository,
    private val appContext: Context
) : ViewModel() {

    data class UiMercadillo(
        val id: String = "",
        val fecha: String = "",
        val lugar: String = "",
        val organizador: String = "",
        val estado: Int = 1,
        val esGratis: Boolean = true,
        val importeSuscripcion: Double = 0.0,
        val saldoInicial: Double = 0.0,
        val totalVentas: Double = 0.0,
        val ventasEfectivo: Double = 0.0,
        val ventasBizum: Double = 0.0,
        val ventasTarjeta: Double = 0.0,
        val totalGastos: Double = 0.0,
        val arqueoCaja: Double? = null,
        val saldoFinal: Double? = null
    )

    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val mercadillo: UiMercadillo? = null,
        val destinosSaldo: List<MercadilloEntity> = emptyList()
    )

    private val _ui = MutableStateFlow(UiState(loading = true))
    val ui: StateFlow<UiState> = _ui

    fun cargar(mercadilloId: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            val lang = ConfigurationManager.idioma.value

            try {
                // Asegura consistencia de totalVentas
                repository.corregirTotalVentasSiIncongruente(mercadilloId)

                // Entity base
                val m = withContext(Dispatchers.IO) { repository.getMercadilloById(mercadilloId) }
                    ?: run {
                        _ui.value = _ui.value.copy(
                            loading = false,
                            error = StringResourceManager.getString("mercadillo_no_encontrado", lang)
                        )
                        return@launch
                    }

                // Ventas por método (desde Recibos)
                val ventasEf = withContext(Dispatchers.IO) { repository.getTotalVentasPorMetodo(mercadilloId, "EFECTIVO") }
                val ventasBi = withContext(Dispatchers.IO) { repository.getTotalVentasPorMetodo(mercadilloId, "BIZUM") }
                val ventasTa = withContext(Dispatchers.IO) { repository.getTotalVentasPorMetodo(mercadilloId, "TARJETA") }

                // Destinos para "Asignar saldo" (estados 1 y 2 del usuario actual)
                val userId = ConfigurationManager.getCurrentUserId() ?: "usuario_default"
                val destinos1 = withContext(Dispatchers.IO) { repository.getMercadillosDeUsuarioPorEstadoSnapshot(userId, 1) }
                val destinos2 = withContext(Dispatchers.IO) { repository.getMercadillosDeUsuarioPorEstadoSnapshot(userId, 2) }

                _ui.value = UiState(
                    loading = false,
                    error = null,
                    mercadillo = UiMercadillo(
                        id = m.idMercadillo,
                        fecha = m.fecha,
                        lugar = m.lugar,
                        organizador = m.organizador,
                        estado = m.estado,
                        esGratis = m.esGratis,
                        importeSuscripcion = if (m.esGratis) 0.0 else m.importeSuscripcion,
                        saldoInicial = m.saldoInicial ?: 0.0,
                        totalVentas = m.totalVentas,
                        ventasEfectivo = ventasEf,
                        ventasBizum = ventasBi,
                        ventasTarjeta = ventasTa,
                        totalGastos = m.totalGastos,
                        arqueoCaja = m.arqueoCaja,
                        saldoFinal = m.saldoFinal
                    ),
                    destinosSaldo = (destinos1 + destinos2).sortedBy { it.fecha }
                )

            } catch (e: Exception) {
                val msg = StringResourceManager
                    .getString("error_cargando_datos", lang)
                    .replace("{0}", e.message ?: "")
                _ui.value = _ui.value.copy(loading = false, error = msg)
            }
        }
    }

    /**
     * Guarda el resultado del arqueo en arqueoCaja y saldoFinal, y avanza el estado:
     * - Premium -> 5 (pendiente de asignar saldo)
     * - Free    -> 6 (cerrado)
     */
    suspend fun confirmarArqueoCaja(mercadilloId: String, saldoFinalCaja: Double) {
        // Usa el getter que SÍ existe en tu ConfigurationManager
        val premium = es.nuskysoftware.marketsales.utils.ConfigurationManager.getIsPremium()
        val nuevoEstado = if (premium) 5 else 6

        withContext(Dispatchers.IO) {
            repository.confirmarArqueoCaja(
                mercadilloId = mercadilloId,
                arqueoFinal = saldoFinalCaja,
                nuevoEstado = nuevoEstado
            )
        }

        // Refresca el UI localmente
        val cur = _ui.value.mercadillo
        if (cur != null && cur.id == mercadilloId) {
            _ui.value = _ui.value.copy(
                mercadillo = cur.copy(
                    estado = nuevoEstado,
                    arqueoCaja = saldoFinalCaja,
                    saldoFinal = saldoFinalCaja
                )
            )
        }
    }

    // ===== Helpers de formato para UI =====
    fun fmtMoneda(value: Double): String {
        val nf = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val simbolo = (ConfigurationManager.moneda.value.split(" ").firstOrNull() ?: "€")
        val raw = nf.format(value)
        return if (!raw.startsWith(simbolo)) raw.replace(Regex("^[^\\d-]+"), "$simbolo ") else raw
    }
}

class ArqueoViewModelFactory(
    private val context: Context,
    private val repository: MercadilloRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArqueoViewModel(repository, context) as T
    }
}
