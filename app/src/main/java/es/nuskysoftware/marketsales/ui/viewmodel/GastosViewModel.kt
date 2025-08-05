// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/GastosViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.repository.GastosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import java.util.UUID

class GastosViewModel(
    private val repo: GastosRepository
) : ViewModel() {

    data class GastoCarritoUI(
        val id: String = UUID.randomUUID().toString(),
        val descripcion: String,
        val importe: Double
    )

    data class UiState(
        val descripcionActual: String = "",
        val importeActual: String = "0,00",
        val lineasCarrito: List<GastoCarritoUI> = emptyList(),
        val totalCarrito: Double = 0.0,
        val saving: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _ui.asStateFlow()

    private var importeCents: Long = 0L

    // Entrada
    fun actualizarDescripcion(texto: String) { _ui.update { it.copy(descripcionActual = texto) } }
    fun onDigitoPresionado(digito: String) {
        if (digito.isEmpty()) return
        val c = digito[0]
        if (!c.isDigit()) return
        val d = (c - '0')
        importeCents = importeCents * 10 + d
        renderImporte()
    }
    fun onDobleDecimalPresionado() { importeCents *= 100; renderImporte() }
    fun onBorrarDigito() { importeCents = max(0, (importeCents / 10).toInt()).toLong(); renderImporte() }
    private fun renderImporte() {
        val euros = importeCents / 100
        val cents = (importeCents % 100).toInt()
        val texto = String.format(Locale("es","ES"), "%d,%02d", euros, cents)
        _ui.update { it.copy(importeActual = texto) }
    }
    fun obtenerImporteComoDouble(): Double = (importeCents.toDouble() / 100.0)

    // Carrito
    fun añadirGastoManual() {
        val desc = _ui.value.descripcionActual.trim()
        val imp = obtenerImporteComoDouble()
        if (desc.isBlank() || imp <= 0.0) return
        val nuevas = _ui.value.lineasCarrito + GastoCarritoUI(descripcion = desc, importe = imp)
        _ui.value = _ui.value.copy(
            lineasCarrito = nuevas,
            totalCarrito = nuevas.sumOf { it.importe },
            descripcionActual = "",
            importeActual = "0,00"
        )
        importeCents = 0L
    }
    fun editarDescripcionLinea(id: String, descripcion: String) {
        _ui.update { it.copy(lineasCarrito = it.lineasCarrito.map { l -> if (l.id == id) l.copy(descripcion = descripcion) else l }) }
    }
    fun editarImporteLinea(id: String, importeTexto: String) {
        val normal = importeTexto.replace('.', ',')
        val parts = normal.split(',')
        val euros = parts.getOrNull(0)?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
        val cents = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val nuevo = euros + (cents / 100.0)
        val nuevas = _ui.value.lineasCarrito.map { if (it.id == id) it.copy(importe = nuevo) else it }
        _ui.value = _ui.value.copy(lineasCarrito = nuevas, totalCarrito = nuevas.sumOf { it.importe })
    }
    fun eliminarLinea(id: String) {
        val nuevas = _ui.value.lineasCarrito.filterNot { it.id == id }
        _ui.value = _ui.value.copy(lineasCarrito = nuevas, totalCarrito = nuevas.sumOf { it.importe })
    }
    fun limpiarCarrito() { _ui.value = _ui.value.copy(lineasCarrito = emptyList(), totalCarrito = 0.0) }

    // Persistencia al elegir método de pago
    fun cargarGastos(mercadilloId: String, metodoPago: String, onFin: (() -> Unit)? = null) {
        val lineas = _ui.value.lineasCarrito
        if (lineas.isEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, message = null) }
            runCatching {
                lineas.forEach { l ->
                    repo.guardarGasto(mercadilloId, l.descripcion.trim(), l.importe, metodoPago).getOrThrow()
                }
            }.onSuccess {
                limpiarCarrito()
                _ui.update { it.copy(saving = false, message = "Gastos cargados") }
                onFin?.invoke()
            }.onFailure { e ->
                _ui.update { it.copy(saving = false, error = e.message ?: "Error guardando gastos") }
            }
        }
    }

    fun formatearImporteEditable(importe: Double): String {
        val euros = importe.toInt()
        val cents = ((importe - euros) * 100).toInt()
        return String.format(Locale("es","ES"), "%d,%02d", euros, cents)
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/GastosViewModel.kt
//package es.nuskysoftware.marketsales.ui.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import es.nuskysoftware.marketsales.data.repository.GastosRepository
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import java.util.Locale
//import kotlin.math.max
//import java.util.UUID
//
//class GastosViewModel(
//    private val repo: GastosRepository
//) : ViewModel() {
//
//    // Item de carrito (no persistido)
//    data class GastoCarritoUI(
//        val id: String = UUID.randomUUID().toString(),
//        val descripcion: String,
//        val importe: Double
//    )
//
//    data class UiState(
//        val descripcionActual: String = "",
//        val importeActual: String = "0,00",
//        val lineasCarrito: List<GastoCarritoUI> = emptyList(),
//        val totalCarrito: Double = 0.0,
//        val saving: Boolean = false,
//        val error: String? = null,
//        val message: String? = null
//    )
//
//    private val _ui = MutableStateFlow(UiState())
//    val uiState: StateFlow<UiState> = _ui.asStateFlow()
//
//    // Mecanismo de teclado 2 decimales (igual que en ventas)
//    private var importeCents: Long = 0L
//
//    // ==== Entrada de datos ====
//    fun actualizarDescripcion(texto: String) {
//        _ui.update { it.copy(descripcionActual = texto) }
//    }
//
//    fun onDigitoPresionado(digito: String) {
//        if (digito.isEmpty()) return
//        val c = digito[0]
//        if (!c.isDigit()) return
//        val d = (c - '0')
//        importeCents = importeCents * 10 + d
//        renderImporte()
//    }
//
//    fun onDobleDecimalPresionado() {
//        importeCents *= 100
//        renderImporte()
//    }
//
//    fun onBorrarDigito() {
//        importeCents = max(0, (importeCents / 10).toInt()).toLong()
//        renderImporte()
//    }
//
//    private fun renderImporte() {
//        // 1234 -> "12,34"
//        val euros = importeCents / 100
//        val cents = (importeCents % 100).toInt()
//        val texto = String.format(Locale("es", "ES"), "%d,%02d", euros, cents)
//        _ui.update { it.copy(importeActual = texto) }
//    }
//
//    fun obtenerImporteComoDouble(): Double =
//        (importeCents.toDouble() / 100.0)
//
//    // ==== Carrito ====
//    fun añadirGastoManual() {
//        val desc = _ui.value.descripcionActual.trim()
//        val imp = obtenerImporteComoDouble()
//        if (desc.isBlank() || imp <= 0.0) return
//
//        val nueva = GastoCarritoUI(descripcion = desc, importe = imp)
//        val nuevas = _ui.value.lineasCarrito + nueva
//        _ui.value = _ui.value.copy(
//            lineasCarrito = nuevas,
//            totalCarrito = nuevas.sumOf { it.importe },
//            descripcionActual = "",
//            // reset teclado:
//            importeActual = "0,00"
//        )
//        importeCents = 0L
//    }
//
//    fun editarDescripcionLinea(id: String, descripcion: String) {
//        val nuevas = _ui.value.lineasCarrito.map {
//            if (it.id == id) it.copy(descripcion = descripcion) else it
//        }
//        _ui.value = _ui.value.copy(lineasCarrito = nuevas)
//    }
//
//    fun editarImporteLinea(id: String, importeTexto: String) {
//        val normal = importeTexto.replace('.', ',')
//        val parts = normal.split(',')
//        val euros = parts.getOrNull(0)?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
//        val cents = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
//        val nuevo = euros + (cents / 100.0)
//
//        val nuevas = _ui.value.lineasCarrito.map {
//            if (it.id == id) it.copy(importe = nuevo) else it
//        }
//        _ui.value = _ui.value.copy(
//            lineasCarrito = nuevas,
//            totalCarrito = nuevas.sumOf { it.importe }
//        )
//    }
//
//    fun eliminarLinea(id: String) {
//        val nuevas = _ui.value.lineasCarrito.filterNot { it.id == id }
//        _ui.value = _ui.value.copy(
//            lineasCarrito = nuevas,
//            totalCarrito = nuevas.sumOf { it.importe }
//        )
//    }
//
//    fun limpiarCarrito() {
//        _ui.value = _ui.value.copy(lineasCarrito = emptyList(), totalCarrito = 0.0)
//    }
//
//    fun formatearImporteEditable(importe: Double): String {
//        // Para el TextField del carrito (simple y robusto)
//        val euros = importe.toInt()
//        val cents = ((importe - euros) * 100).toInt()
//        return String.format(Locale("es","ES"), "%d,%02d", euros, cents)
//    }
//
//    // ==== Persistencia al pulsar "Cargar Gasto" ====
//    fun cargarGastos(mercadilloId: String, /* metodoPago: MetodoPago */ onFin: (() -> Unit)? = null) {
//        val lineas = _ui.value.lineasCarrito
//        if (lineas.isEmpty()) return
//
//        viewModelScope.launch {
//            _ui.update { it.copy(saving = true, error = null, message = null) }
//            runCatching {
//                // Por ahora guardamos sin forma de pago (se añadirá en migración del paso 4)
//                lineas.forEach { l ->
//                    repo.guardarGasto(mercadilloId, l.descripcion.trim(), l.importe)
//                        .getOrThrow()
//                }
//            }.onSuccess {
//                limpiarCarrito()
//                _ui.update { it.copy(saving = false, message = "Gastos cargados") }
//                onFin?.invoke()
//            }.onFailure { e ->
//                _ui.update { it.copy(saving = false, error = e.message ?: "Error guardando gastos") }
//            }
//        }
//    }
//}
//
