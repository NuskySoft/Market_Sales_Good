package es.nuskysoftware.marketsales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.cache.CarritoCache
import es.nuskysoftware.marketsales.data.local.cache.DraftCarrito
import es.nuskysoftware.marketsales.data.repository.LineaVentaUI
import es.nuskysoftware.marketsales.data.repository.MetodoPago
import es.nuskysoftware.marketsales.data.repository.PestanaVenta
import es.nuskysoftware.marketsales.data.repository.TipoLinea
import es.nuskysoftware.marketsales.data.repository.VentasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VentasUiState(
    val pestanaActiva: PestanaVenta = PestanaVenta.MANUAL,
    val lineasTicket: List<LineaVentaUI> = emptyList(),
    val totalTicket: Double = 0.0,
    val importeActual: String = "0,00",
    val descripcionActual: String = "",
    val filtroCategoria: String = "",
    val terminoBusqueda: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class VentasViewModel(
    private val repository: VentasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    private var mercadilloId: String = ""
    private var usuarioId: String = ""

    // ---------- Inicialización de contexto ----------
    fun inicializar(mercadilloId: String, usuarioId: String) {
        this.mercadilloId = mercadilloId
        this.usuarioId = usuarioId

        // Restaurar borrador si existe
        CarritoCache.load(mercadilloId)?.let { draft ->
            _uiState.value = _uiState.value.copy(
                pestanaActiva = draft.pestana,
                lineasTicket = draft.lineas,
                totalTicket = draft.total,
                importeActual = draft.importeActual,
                descripcionActual = draft.descripcionActual
            )
        }
    }

    // ---------- Persistencia en caché ----------
    private fun persistirSiProcede() {
        if (mercadilloId.isBlank()) return
        val s = _uiState.value
        if (s.lineasTicket.isEmpty()) {
            CarritoCache.clear(mercadilloId)
        } else {
            CarritoCache.save(
                mercadilloId,
                DraftCarrito(
                    lineas = s.lineasTicket,
                    total = s.totalTicket,
                    importeActual = s.importeActual,
                    descripcionActual = s.descripcionActual,
                    pestana = s.pestanaActiva
                )
            )
        }
    }

    private fun actualizarState(nuevo: VentasUiState) {
        _uiState.value = nuevo
        persistirSiProcede()
    }

    // ---------- Navegación entre pestañas ----------
    fun cambiarPestana(nuevaPestana: PestanaVenta) {
        actualizarState(_uiState.value.copy(pestanaActiva = nuevaPestana))
    }

    // ---------- Teclado numérico (2 decimales fijos) ----------
    fun onDigitoPresionado(digito: String) {
        val nuevoImporte = calcularNuevoImporteTPV(_uiState.value.importeActual, digito)
        actualizarState(_uiState.value.copy(importeActual = nuevoImporte))
    }

    fun onDobleDecimalPresionado() {
        val paso1 = calcularNuevoImporteTPV(_uiState.value.importeActual, "0")
        val paso2 = calcularNuevoImporteTPV(paso1, "0")
        actualizarState(_uiState.value.copy(importeActual = paso2))
    }

    fun onBorrarDigito() {
        val nuevoImporte = retrocederDigitoTPV(_uiState.value.importeActual)
        actualizarState(_uiState.value.copy(importeActual = nuevoImporte))
    }

    private fun calcularNuevoImporteTPV(importeActual: String, nuevoDigito: String): String {
        val soloNumeros = importeActual.replace(",", "").replace(".", "")
        val numerosConNuevo = soloNumeros + nuevoDigito
        val numerosFinal = if (numerosConNuevo.length > 7) numerosConNuevo.takeLast(7) else numerosConNuevo
        return formatearComoImporte(numerosFinal)
    }

    private fun retrocederDigitoTPV(importeActual: String): String {
        val soloNumeros = importeActual.replace(",", "").replace(".", "")
        return if (soloNumeros.length <= 1) "0,00"
        else formatearComoImporte(soloNumeros.dropLast(1))
    }

    private fun formatearComoImporte(numeros: String): String {
        val numerosCompletos = numeros.padStart(3, '0')
        val decimales = numerosCompletos.takeLast(2)
        val enteros = numerosCompletos.dropLast(2)
        val enterosLimpios = enteros.toLongOrNull()?.toString() ?: "0"
        return "$enterosLimpios,$decimales"
    }

    fun obtenerImporteComoDouble(): Double =
        _uiState.value.importeActual.replace(",", ".").toDoubleOrNull() ?: 0.0

    // ---------- Descripción ----------
    fun actualizarDescripcion(nuevaDescripcion: String) {
        actualizarState(_uiState.value.copy(descripcionActual = nuevaDescripcion))
    }

    // ---------- Carrito: añadir / eliminar / editar ----------
    fun añadirLineaManual() {
        val descripcion = _uiState.value.descripcionActual.trim()
        val importe = obtenerImporteComoDouble()
        if (descripcion.isBlank()) { mostrarError("La descripción es obligatoria"); return }
        if (importe <= 0.0) { mostrarError("El importe debe ser mayor que 0"); return }

        val nuevaLinea = LineaVentaUI(
            tipoLinea = TipoLinea.MANUAL,
            descripcion = descripcion,
            cantidad = 1,
            precioUnitario = importe,
            subtotal = importe
        )
        val lineasActualizadas = _uiState.value.lineasTicket + nuevaLinea
        actualizarState(
            _uiState.value.copy(
                lineasTicket = lineasActualizadas,
                totalTicket = lineasActualizadas.sumOf { it.subtotal },
                importeActual = "0,00",
                descripcionActual = ""
            )
        )
    }

    fun eliminarLinea(lineaId: String) {
        val nuevas = _uiState.value.lineasTicket.filter { it.id != lineaId }
        actualizarState(_uiState.value.copy(lineasTicket = nuevas, totalTicket = nuevas.sumOf { it.subtotal }))
    }

    fun editarCantidadLinea(idLinea: String, nuevaCantidad: Int) {
        val actuales = _uiState.value.lineasTicket
        val nuevas = actuales.map { l ->
            if (l.id == idLinea) {
                if (nuevaCantidad <= 0) null
                else l.copy(cantidad = nuevaCantidad, subtotal = l.precioUnitario * nuevaCantidad)
            } else l
        }.filterNotNull()
        actualizarState(_uiState.value.copy(lineasTicket = nuevas, totalTicket = nuevas.sumOf { it.subtotal }))
    }

    fun editarPrecioUnitarioLinea(idLinea: String, nuevoPrecio: Double) {
        if (nuevoPrecio < 0) return
        val nuevas = _uiState.value.lineasTicket.map { l ->
            if (l.id == idLinea) l.copy(precioUnitario = nuevoPrecio, subtotal = nuevoPrecio * l.cantidad) else l
        }
        actualizarState(_uiState.value.copy(lineasTicket = nuevas, totalTicket = nuevas.sumOf { it.subtotal }))
    }

    fun añadirProducto(idProducto: String, descripcion: String, precio: Double) {
        val actuales = _uiState.value.lineasTicket
        val indice = actuales.indexOfLast { it.tipoLinea == TipoLinea.PRODUCTO && it.idProducto == idProducto }
        val nuevas = if (indice >= 0) {
            val l = actuales[indice]; val q = l.cantidad + 1
            actuales.toMutableList().apply {
                set(indice, l.copy(cantidad = q, subtotal = q * l.precioUnitario))
            }
        } else {
            actuales + LineaVentaUI(
                tipoLinea = TipoLinea.PRODUCTO,
                descripcion = descripcion,
                idProducto = idProducto,
                cantidad = 1,
                precioUnitario = precio,
                subtotal = precio
            )
        }
        actualizarState(_uiState.value.copy(lineasTicket = nuevas, totalTicket = nuevas.sumOf { it.subtotal }))
    }

    // ---------- Finalizar venta ----------
    fun finalizarVenta(metodoPago: MetodoPago) {
        val s = _uiState.value
        if (s.lineasTicket.isEmpty()) { mostrarError("No hay líneas para vender"); return }
        if (s.totalTicket <= 0.0) { mostrarError("El total debe ser mayor que 0"); return }
        if (mercadilloId.isBlank()) { mostrarError("Mercadillo no inicializado"); return }

        viewModelScope.launch {
            _uiState.value = s.copy(loading = true, error = null)
            try {
                val resultado = repository.guardarVenta(
                    idMercadillo = mercadilloId,
                    lineas = s.lineasTicket,
                    metodoPago = metodoPago.name,
                    total = s.totalTicket
                )
                if (resultado.isSuccess) {
                    CarritoCache.clear(mercadilloId)
                    limpiarVenta()
                } else {
                    mostrarError("Error al guardar la venta: ${resultado.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                mostrarError("Error al guardar la venta: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(loading = false)
            }
        }
    }

    fun sincronizarPendientes() {
        viewModelScope.launch {
            try { /* repository.sincronizarPendientes() */ } catch (_: Exception) {}
        }
    }

    private fun limpiarVenta() {
        CarritoCache.clear(mercadilloId)
        _uiState.value = VentasUiState(pestanaActiva = _uiState.value.pestanaActiva)
    }

    // ---------- Errores ----------
    private fun mostrarError(mensaje: String) {
        _uiState.value = _uiState.value.copy(error = mensaje)
    }
    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ---------- Filtros / búsqueda ----------
    fun actualizarTerminoBusqueda(termino: String) {
        actualizarState(_uiState.value.copy(terminoBusqueda = termino))
    }
    fun actualizarFiltroCategoria(categoria: String) {
        actualizarState(_uiState.value.copy(filtroCategoria = categoria))
    }
}


//// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/VentasViewModel.kt
//
//package es.nuskysoftware.marketsales.ui.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import es.nuskysoftware.marketsales.data.repository.LineaVentaUI
//import es.nuskysoftware.marketsales.data.repository.MetodoPago
//import es.nuskysoftware.marketsales.data.repository.PestanaVenta
//import es.nuskysoftware.marketsales.data.repository.TipoLinea
//import es.nuskysoftware.marketsales.data.repository.VentasRepository
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//data class VentasUiState(
//    val pestanaActiva: PestanaVenta = PestanaVenta.MANUAL,
//    val lineasTicket: List<LineaVentaUI> = emptyList(),
//    val totalTicket: Double = 0.0,
//    val importeActual: String = "0,00",
//    val descripcionActual: String = "",
//    val filtroCategoria: String = "",
//    val terminoBusqueda: String = "",
//    val loading: Boolean = false,
//    val error: String? = null
//)
//
///**
// * ViewModel de Ventas
// * - Mantiene el carrito (líneas) y el total.
// * - Lógica del teclado numérico estilo TPV (2 decimales fijos).
// * - Finalización de venta: guarda en Room vía Repository y dispara la sincronización offline-first.
// *
// * NOTA: El Factory va en archivo separado (VentasViewModelFactory.kt).
// */
//class VentasViewModel(
//    private val repository: VentasRepository
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(VentasUiState())
//    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()
//
//    // Contexto de la venta (inyectado por la pantalla contenedora)
//    private var mercadilloId: String = ""
//    private var usuarioId: String = "" // reservado si lo necesitas para metadata
//
//    // ---------- Inicialización de contexto ----------
//    fun inicializar(mercadilloId: String, usuarioId: String) {
//        this.mercadilloId = mercadilloId
//        this.usuarioId = usuarioId
//    }
//
//    // ---------- Navegación entre pestañas ----------
//    fun cambiarPestana(nuevaPestana: PestanaVenta) {
//        _uiState.value = _uiState.value.copy(pestanaActiva = nuevaPestana)
//    }
//
//    // ---------- Teclado numérico estilo TPV (2 decimales fijos) ----------
//    fun onDigitoPresionado(digito: String) {
//        val importeActual = _uiState.value.importeActual
//        val nuevoImporte = calcularNuevoImporteTPV(importeActual, digito)
//        _uiState.value = _uiState.value.copy(importeActual = nuevoImporte)
//    }
//
//    fun onDobleDecimalPresionado() {
//        val paso1 = calcularNuevoImporteTPV(_uiState.value.importeActual, "0")
//        val paso2 = calcularNuevoImporteTPV(paso1, "0")
//        _uiState.value = _uiState.value.copy(importeActual = paso2)
//    }
//
//    fun onBorrarDigito() {
//        val nuevoImporte = retrocederDigitoTPV(_uiState.value.importeActual)
//        _uiState.value = _uiState.value.copy(importeActual = nuevoImporte)
//    }
//
//    private fun calcularNuevoImporteTPV(importeActual: String, nuevoDigito: String): String {
//        // Normaliza coma/punto y quita separadores para tratar todo como dígitos
//        val soloNumeros = importeActual.replace(",", "").replace(".", "")
//        val numerosConNuevo = soloNumeros + nuevoDigito
//        // Límite 7 dígitos -> 99999,99
//        val numerosFinal = if (numerosConNuevo.length > 7) {
//            numerosConNuevo.takeLast(7)
//        } else numerosConNuevo
//        return formatearComoImporte(numerosFinal)
//    }
//
//    private fun retrocederDigitoTPV(importeActual: String): String {
//        val soloNumeros = importeActual.replace(",", "").replace(".", "")
//        return if (soloNumeros.length <= 1) {
//            "0,00"
//        } else {
//            val numerosSinUltimo = soloNumeros.dropLast(1)
//            formatearComoImporte(numerosSinUltimo)
//        }
//    }
//
//    private fun formatearComoImporte(numeros: String): String {
//        val numerosCompletos = numeros.padStart(3, '0')
//        val decimales = numerosCompletos.takeLast(2)
//        val enteros = numerosCompletos.dropLast(2)
//        val enterosLimpios = enteros.toLongOrNull()?.toString() ?: "0"
//        return "$enterosLimpios,$decimales"
//    }
//
//    fun obtenerImporteComoDouble(): Double {
//        return _uiState.value.importeActual.replace(",", ".").toDoubleOrNull() ?: 0.0
//    }
//
//    // ---------- Descripción ----------
//    fun actualizarDescripcion(nuevaDescripcion: String) {
//        _uiState.value = _uiState.value.copy(descripcionActual = nuevaDescripcion)
//    }
//
//    // ---------- Carrito: añadir línea manual ----------
//    fun añadirLineaManual() {
//        val descripcion = _uiState.value.descripcionActual.trim()
//        val importe = obtenerImporteComoDouble()
//
//        if (descripcion.isBlank()) {
//            mostrarError("La descripción es obligatoria")
//            return
//        }
//        if (importe <= 0.0) {
//            mostrarError("El importe debe ser mayor que 0")
//            return
//        }
//
//        val nuevaLinea = LineaVentaUI(
//            tipoLinea = TipoLinea.MANUAL,
//            descripcion = descripcion,
//            cantidad = 1,
//            precioUnitario = importe,
//            subtotal = importe
//        )
//
//        val lineasActualizadas = _uiState.value.lineasTicket + nuevaLinea
//        _uiState.value = _uiState.value.copy(
//            lineasTicket = lineasActualizadas,
//            totalTicket = calcularTotal(lineasActualizadas)
//        )
//
//        resetearCampos()
//    }
//
//    fun eliminarLinea(lineaId: String) {
//        val lineasActualizadas = _uiState.value.lineasTicket.filter { it.id != lineaId }
//        _uiState.value = _uiState.value.copy(
//            lineasTicket = lineasActualizadas,
//            totalTicket = calcularTotal(lineasActualizadas)
//        )
//    }
//
//    private fun calcularTotal(lineas: List<LineaVentaUI>): Double =
//        lineas.sumOf { it.subtotal }
//
//    fun resetearCampos() {
//        _uiState.value = _uiState.value.copy(
//            importeActual = "0,00",
//            descripcionActual = ""
//        )
//    }
//
//    // ---------- Finalizar venta (Room + trigger sync offline-first) ----------
//    /**
//     * Guarda la venta en Room a través del Repository (híbrido offline-first).
//     * - Requiere que `inicializar(mercadilloId, usuarioId)` se haya llamado antes.
//     * - Al completar correctamente, limpia el carrito.
//     */
//    fun finalizarVenta(metodoPago: MetodoPago) {
//        val state = _uiState.value
//        if (state.lineasTicket.isEmpty()) {
//            mostrarError("No hay líneas para vender")
//            return
//        }
//        if (state.totalTicket <= 0.0) {
//            mostrarError("El total debe ser mayor que 0")
//            return
//        }
//        if (mercadilloId.isBlank()) {
//            mostrarError("Mercadillo no inicializado")
//            return
//        }
//
//        viewModelScope.launch {
//            _uiState.value = state.copy(loading = true, error = null)
//            try {
//                val resultado = repository.guardarVenta(
//                    idMercadillo = mercadilloId,
//                    lineas = state.lineasTicket,
//                    metodoPago = metodoPago.name,
//                    total = state.totalTicket
//                )
//                if (resultado.isSuccess) {
//                    limpiarVenta()
//                } else {
//                    mostrarError("Error al guardar la venta: ${resultado.exceptionOrNull()?.message}")
//                }
//            } catch (e: Exception) {
//                mostrarError("Error al guardar la venta: ${e.message}")
//            } finally {
//                _uiState.value = _uiState.value.copy(loading = false)
//            }
//        }
//    }
//
//    /**
//     * Dispara la sincronización de ventas pendientes (si hay conectividad).
//     * Seguro de llamar: encapsulado en try/catch.
//     */
//    fun sincronizarPendientes() {
//        viewModelScope.launch {
//            try {
//               // repository.sincronizarPendientes()
//            } catch (_: Exception) {
//                // Silencioso: el reintento ocurrirá más tarde.
//            }
//        }
//    }
//
//    private fun limpiarVenta() {
//        // Conserva la pestaña activa; resetea el resto
//        _uiState.value = VentasUiState(
//            pestanaActiva = _uiState.value.pestanaActiva
//        )
//    }
//
//    // ---------- Errores ----------
//    private fun mostrarError(mensaje: String) {
//        _uiState.value = _uiState.value.copy(error = mensaje)
//    }
//
//    fun limpiarError() {
//        _uiState.value = _uiState.value.copy(error = null)
//    }
//
//    // ---------- Filtros / búsqueda (para pestaña productos) ----------
//    fun actualizarTerminoBusqueda(termino: String) {
//        _uiState.value = _uiState.value.copy(terminoBusqueda = termino)
//    }
//
//    fun actualizarFiltroCategoria(categoria: String) {
//        _uiState.value = _uiState.value.copy(filtroCategoria = categoria)
//    }
//
//    fun añadirProducto(idProducto: String, descripcion: String, precio: Double) {
//        val actuales = _uiState.value.lineasTicket
//
//        // Busca última línea del mismo producto para incrementar cantidad
//        val indice = actuales.indexOfLast { it.tipoLinea == TipoLinea.PRODUCTO && it.idProducto == idProducto }
//
//        val nuevasLineas = if (indice >= 0) {
//            val linea = actuales[indice]
//            val nuevaCantidad = linea.cantidad + 1
//            actuales.toMutableList().apply {
//                set(
//                    indice,
//                    linea.copy(
//                        cantidad = nuevaCantidad,
//                        subtotal = nuevaCantidad * linea.precioUnitario
//                    )
//                )
//            }
//        } else {
//            actuales + LineaVentaUI(
//                tipoLinea = TipoLinea.PRODUCTO,
//                descripcion = descripcion,
//                idProducto = idProducto,
//                cantidad = 1,
//                precioUnitario = precio,
//                subtotal = precio
//            )
//        }
//
//        _uiState.value = _uiState.value.copy(
//            lineasTicket = nuevasLineas,
//            totalTicket = nuevasLineas.sumOf { it.subtotal }
//        )
//    }
//
//    // app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/VentasViewModel.kt
//// dentro de class VentasViewModel { ... }
//
//    // Editar cantidad
//    fun editarCantidadLinea(idLinea: String, nuevaCantidad: Int) {
//        val actuales = _uiState.value.lineasTicket
//        val nuevas = actuales.map { l ->
//            if (l.id == idLinea) {
//                if (nuevaCantidad <= 0) {
//                    null // eliminar
//                } else {
//                    l.copy(
//                        cantidad = nuevaCantidad,
//                        subtotal = l.precioUnitario * nuevaCantidad
//                    )
//                }
//            } else l
//        }.filterNotNull()
//        _uiState.value = _uiState.value.copy(
//            lineasTicket = nuevas,
//            totalTicket = nuevas.sumOf { it.subtotal }
//        )
//    }
//
//    // Editar precio unitario
//    fun editarPrecioUnitarioLinea(idLinea: String, nuevoPrecio: Double) {
//        if (nuevoPrecio < 0) return
//        val actuales = _uiState.value.lineasTicket
//        val nuevas = actuales.map { l ->
//            if (l.id == idLinea) {
//                val nuevoSubtotal = nuevoPrecio * l.cantidad
//                l.copy(precioUnitario = nuevoPrecio, subtotal = nuevoSubtotal)
//            } else l
//        }
//        _uiState.value = _uiState.value.copy(
//            lineasTicket = nuevas,
//            totalTicket = nuevas.sumOf { it.subtotal }
//        )
//    }
//
//}
//
