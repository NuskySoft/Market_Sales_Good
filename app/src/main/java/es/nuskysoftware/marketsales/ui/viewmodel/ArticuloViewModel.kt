// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/ArticuloViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.ArticuloEntity
import es.nuskysoftware.marketsales.data.repository.ArticuloRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ArticuloViewModel V11 - Market Sales
 *
 * ARQUITECTURA REACTIVA:
 * - StateFlow para estados reactivos
 * - Observa cambios en tiempo real
 * - Compatible con sistema híbrido "Reloj Suizo"
 * - Estados de UI para loading, errores, etc.
 * - Validaciones Premium para campos de coste y stock
 */
class ArticuloViewModel(
    private val repository: ArticuloRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ArticuloViewModel"
    }

    // ========== ESTADOS REACTIVOS ==========

    private val _uiState = MutableStateFlow(ArticuloUiState())
    val uiState: StateFlow<ArticuloUiState> = _uiState.asStateFlow()

    // Artículos del usuario actual en tiempo real
    val articulos = repository.getArticulosUsuarioActual()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        Log.d(TAG, "✅ ArticuloViewModel inicializado")
    }

    // ========== OPERACIONES CRUD ==========

    /**
     * Crea un nuevo artículo
     */
    fun crearArticulo(
        nombre: String,
        idCategoria: String,
        precioVenta: Double,
        precioCoste: Double? = null,
        stock: Int? = null,
        controlarStock: Boolean = false,
        controlarCoste: Boolean = false,
        favorito: Boolean = false,
        fotoUri: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                // Validar datos básicos
                val validacionNombre = validarNombreArticulo(nombre)
                if (validacionNombre != null) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = validacionNombre
                    )
                    return@launch
                }

                val validacionPrecio = validarPrecioVenta(precioVenta)
                if (validacionPrecio != null) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = validacionPrecio
                    )
                    return@launch
                }

                // Validar nombre duplicado
                if (repository.existeArticuloConNombre(nombre)) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Ya existe un artículo con ese nombre"
                    )
                    return@launch
                }

                // Validar campos Premium si están activos
                if (controlarCoste && precioCoste != null) {
                    val validacionCoste = validarPrecioCoste(precioCoste)
                    if (validacionCoste != null) {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            error = validacionCoste
                        )
                        return@launch
                    }
                }

                if (controlarStock && stock != null) {
                    val validacionStock = validarStock(stock)
                    if (validacionStock != null) {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            error = validacionStock
                        )
                        return@launch
                    }
                }

                val articuloId = repository.crearArticulo(
                    nombre = nombre,
                    idCategoria = idCategoria,
                    precioVenta = precioVenta,
                    precioCoste = precioCoste,
                    stock = stock,
                    controlarStock = controlarStock,
                    controlarCoste = controlarCoste,
                    favorito = favorito,
                    fotoUri = fotoUri
                )

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = "Artículo creado exitosamente",
                    error = null
                )

                Log.d(TAG, "✅ Artículo creado: $nombre (ID: $articuloId)")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Error creando artículo: ${e.message}"
                )
                Log.e(TAG, "❌ Error creando artículo", e)
            }
        }
    }

    /**
     * Actualiza un artículo existente
     */
    fun actualizarArticulo(articulo: ArticuloEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                // Validar datos básicos
                val validacionNombre = validarNombreArticulo(articulo.nombre)
                if (validacionNombre != null) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = validacionNombre
                    )
                    return@launch
                }

                val validacionPrecio = validarPrecioVenta(articulo.precioVenta)
                if (validacionPrecio != null) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = validacionPrecio
                    )
                    return@launch
                }

                // Validar nombre duplicado (excluyendo el artículo actual)
                if (repository.existeArticuloConNombre(articulo.nombre, articulo.idArticulo)) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Ya existe un artículo con ese nombre"
                    )
                    return@launch
                }

                // Validar campos Premium si están activos
                if (articulo.controlarCoste && articulo.precioCoste != null) {
                    val validacionCoste = validarPrecioCoste(articulo.precioCoste)
                    if (validacionCoste != null) {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            error = validacionCoste
                        )
                        return@launch
                    }
                }

                if (articulo.controlarStock && articulo.stock != null) {
                    val validacionStock = validarStock(articulo.stock)
                    if (validacionStock != null) {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            error = validacionStock
                        )
                        return@launch
                    }
                }

                val exito = repository.actualizarArticulo(articulo)

                if (exito) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = "Artículo actualizado exitosamente",
                        error = null
                    )
                    Log.d(TAG, "✅ Artículo actualizado: ${articulo.nombre}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Error actualizando artículo"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Error actualizando artículo: ${e.message}"
                )
                Log.e(TAG, "❌ Error actualizando artículo", e)
            }
        }
    }

    /**
     * Elimina un artículo
     */
    fun eliminarArticulo(articulo: ArticuloEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                val exito = repository.eliminarArticulo(articulo)

                if (exito) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = "Artículo eliminado exitosamente",
                        error = null
                    )
                    Log.d(TAG, "✅ Artículo eliminado: ${articulo.nombre}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Error eliminando artículo"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Error eliminando artículo: ${e.message}"
                )
                Log.e(TAG, "❌ Error eliminando artículo", e)
            }
        }
    }

    /**
     * Obtiene un artículo por ID
     */
    suspend fun getArticuloById(id: String): ArticuloEntity? {
        return try {
            repository.getArticuloById(id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo artículo por ID: $id", e)
            null
        }
    }

    /**
     * Obtiene artículos filtrados por categoría
     */
    fun getArticulosByCategoria(categoriaId: String): Flow<List<ArticuloEntity>> {
        return repository.getArticulosByCategoria(categoriaId)
    }

    /**
     * Busca artículos por nombre
     */
    fun searchArticulos(query: String): Flow<List<ArticuloEntity>> {
        return repository.searchArticulos(query)
    }

    // ========== SINCRONIZACIÓN ==========

    /**
     * Fuerza sincronización manual
     */
    fun forzarSincronizacion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                val exito = repository.forzarSincronizacion()

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = if (exito) "Sincronización completada" else "Error en sincronización",
                    error = if (!exito) "No se pudo completar la sincronización" else null
                )

                Log.d(TAG, if (exito) "✅ Sincronización forzada exitosa" else "⚠️ Error en sincronización forzada")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Error en sincronización: ${e.message}"
                )
                Log.e(TAG, "❌ Error en sincronización forzada", e)
            }
        }
    }

    // ========== GESTIÓN DE UI ==========

    /**
     * Limpia mensajes de error o éxito
     */
    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(
            error = null,
            message = null
        )
    }

    /**
     * Limpia solo el mensaje de error
     */
    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Limpia solo el mensaje de éxito
     */
    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    // ========== VALIDACIONES ==========

    /**
     * Valida si un nombre de artículo es válido
     */
    fun validarNombreArticulo(nombre: String): String? {
        return when {
            nombre.isBlank() -> "El nombre no puede estar vacío"
            nombre.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            nombre.length > 100 -> "El nombre no puede tener más de 100 caracteres"
            else -> null
        }
    }

    /**
     * Valida si un precio de venta es válido
     */
    fun validarPrecioVenta(precio: Double): String? {
        return when {
            precio < 0 -> "El precio no puede ser negativo"
            precio > 999999.99 -> "El precio es demasiado alto"
            else -> null
        }
    }

    /**
     * Valida si un precio de coste es válido
     */
    fun validarPrecioCoste(precio: Double): String? {
        return when {
            precio < 0 -> "El precio de coste no puede ser negativo"
            precio > 999999.99 -> "El precio de coste es demasiado alto"
            else -> null
        }
    }

    /**
     * Valida si un stock es válido
     */
    fun validarStock(stock: Int): String? {
        return when {
            stock < 0 -> "El stock no puede ser negativo"
            stock > 999999 -> "El stock es demasiado alto"
            else -> null
        }
    }

    // ========== UTILIDADES ==========

    /**
     * Obtiene el número total de artículos del usuario
     */
    val totalArticulos: StateFlow<Int> = articulos
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Indica si hay artículos creados
     */
    val tieneArticulos: StateFlow<Boolean> = articulos
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Artículos favoritos
     */
    val articulosFavoritos: StateFlow<List<ArticuloEntity>> = articulos
        .map { lista -> lista.filter { it.favorito } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Artículos con stock bajo (menos de 5 unidades)
     */
    val articulosStockBajo: StateFlow<List<ArticuloEntity>> = articulos
        .map { lista ->
            lista.filter { articulo ->
                articulo.controlarStock && articulo.stock != null && articulo.stock < 5
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Estado de la UI del ArticuloViewModel
 */
data class ArticuloUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)