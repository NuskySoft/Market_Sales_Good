// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/VentasViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.cache.CarritoCache
import es.nuskysoftware.marketsales.data.local.cache.DraftCarrito
import es.nuskysoftware.marketsales.data.repository.ArticuloRepository
import es.nuskysoftware.marketsales.data.repository.LineaVentaUI
import es.nuskysoftware.marketsales.data.repository.MetodoPago
import es.nuskysoftware.marketsales.data.repository.PestanaVenta
import es.nuskysoftware.marketsales.data.repository.TipoLinea
import es.nuskysoftware.marketsales.data.repository.VentasRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
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
    private val repository: VentasRepository,
    private val articuloRepository: ArticuloRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    private var mercadilloId: String = ""
    private var usuarioId: String = ""

    // Idioma actual (para StringResourceManager)
    private val currentLanguage get() = ConfigurationManager.idioma.value

    // Helper corto para textos
    private fun t(key: String) = StringResourceManager.getString(key, currentLanguage)

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
        if (descripcion.isBlank()) { mostrarError(t("descripcion_obligatoria")); return }
        if (importe <= 0.0) { mostrarError(t("importe_mayor_cero")); return }

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
        if (s.lineasTicket.isEmpty()) { mostrarError(t("no_hay_lineas_vender")); return }
        if (s.totalTicket <= 0.0) { mostrarError(t("total_mayor_cero")); return }
        if (mercadilloId.isBlank()) { mostrarError(t("mercadillo_no_inicializado")); return }

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
                    if (ConfigurationManager.getIsPremium() && s.pestanaActiva == PestanaVenta.PRODUCTOS) {
                        s.lineasTicket.filter { it.tipoLinea == TipoLinea.PRODUCTO && it.idProducto != null }
                            .forEach { linea ->
                                try {
                                    val articulo = articuloRepository.getArticuloById(linea.idProducto!!)
                                    if (articulo != null && articulo.controlarStock) {
                                        val nuevoStock = (articulo.stock ?: 0) - linea.cantidad
                                        articuloRepository.actualizarArticulo(articulo.copy(stock = nuevoStock))
                                    }
                                } catch (_: Exception) { }
                            }
                    }
                    CarritoCache.clear(mercadilloId)
                    limpiarVenta()
                } else {
                    val base = t("error_guardar_venta")
                    mostrarError(base.replace("{0}", resultado.exceptionOrNull()?.message ?: ""))
                }
            } catch (e: Exception) {
                val base = t("error_guardar_venta")
                mostrarError(base.replace("{0}", e.message ?: ""))
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
