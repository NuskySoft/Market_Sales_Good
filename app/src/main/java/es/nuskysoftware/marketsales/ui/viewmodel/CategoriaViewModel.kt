// app/src/main/java/es/nuskysoftware/marketsales/data/viewmodel/CategoriaViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.data.repository.CategoriaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * CategoriaViewModel V11 - Market Sales
 *
 * ARQUITECTURA REACTIVA:
 * - StateFlow para estados reactivos
 * - Observa cambios en tiempo real
 * - Compatible con sistema híbrido "Reloj Suizo"
 * - Estados de UI para loading, errores, etc.
 */
class CategoriaViewModel(
    private val repository: CategoriaRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CategoriaViewModel"
    }

    // ========== ESTADOS REACTIVOS ==========

    private val _uiState = MutableStateFlow(CategoriaUiState())
    val uiState: StateFlow<CategoriaUiState> = _uiState.asStateFlow()

    // Categorías del usuario actual en tiempo real
    val categorias = repository.getCategoriasUsuarioActual()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        Log.d(TAG, "✅ CategoriaViewModel inicializado")
    }

    // ========== OPERACIONES CRUD ==========

    /**
     * Crea una nueva categoría
     */
    fun crearCategoria(
        nombre: String,
        colorHex: String,
        orden: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                // Validar nombre duplicado
                if (repository.existeCategoriaConNombre(nombre)) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Ya existe una categoría con ese nombre"
                    )
                    return@launch
                }

                val categoriaId = repository.crearCategoria(nombre, colorHex, orden)

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = "Categoría creada exitosamente",
                    error = null
                )

                Log.d(TAG, "✅ Categoría creada: $nombre (ID: $categoriaId)")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Error creando categoría: ${e.message}"
                )
                Log.e(TAG, "❌ Error creando categoría", e)
            }
        }
    }

    /**
     * Actualiza una categoría existente
     */
    fun actualizarCategoria(categoria: CategoriaEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                // Validar nombre duplicado (excluyendo la categoría actual)
                if (repository.existeCategoriaConNombre(categoria.nombre, categoria.idCategoria)) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Ya existe una categoría con ese nombre"
                    )
                    return@launch
                }

                val exito = repository.actualizarCategoria(categoria)

                if (exito) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = "Categoría actualizada exitosamente",
                        error = null
                    )
                    Log.d(TAG, "✅ Categoría actualizada: ${categoria.nombre}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Error actualizando categoría"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Error actualizando categoría: ${e.message}"
                )
                Log.e(TAG, "❌ Error actualizando categoría", e)
            }
        }
    }

    /**
     * Elimina una categoría
     */
    fun eliminarCategoria(categoria: CategoriaEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                val exito = repository.eliminarCategoria(categoria)

                if (exito) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = "Categoría eliminada exitosamente",
                        error = null
                    )
                    Log.d(TAG, "✅ Categoría eliminada: ${categoria.nombre}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Error eliminando categoría"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Error eliminando categoría: ${e.message}"
                )
                Log.e(TAG, "❌ Error eliminando categoría", e)
            }
        }
    }

    /**
     * Obtiene una categoría por ID
     */
    suspend fun getCategoriaById(id: String): CategoriaEntity? {
        return try {
            repository.getCategoriaById(id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo categoría por ID: $id", e)
            null
        }
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
     * Valida si un nombre de categoría es válido
     */
    fun validarNombreCategoria(nombre: String): String? {
        return when {
            nombre.isBlank() -> "El nombre no puede estar vacío"
            nombre.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            nombre.length > 50 -> "El nombre no puede tener más de 50 caracteres"
            else -> null
        }
    }

    /**
     * Valida si un color hex es válido
     */
    fun validarColorHex(colorHex: String): String? {
        return if (colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            null
        } else {
            "Color inválido"
        }
    }

    // ========== UTILIDADES ==========

    /**
     * Obtiene el número total de categorías del usuario
     */
    val totalCategorias: StateFlow<Int> = categorias
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Indica si hay categorías creadas
     */
    val tieneCategorias: StateFlow<Boolean> = categorias
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

/**
 * Estado de la UI del CategoriaViewModel
 */
data class CategoriaUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
