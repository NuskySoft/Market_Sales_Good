// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/CategoriaViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.data.repository.CategoriaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

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

    // Acceso cómodo a traducciones con placeholders {0}, {1}, …
    private fun tr(key: String, vararg args: String): String {
        var txt = StringResourceManager.getString(key, ConfigurationManager.idioma.value)
        args.forEachIndexed { i, a -> txt = txt.replace("{$i}", a) }
        return txt
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
                        error = tr("categoria_nombre_duplicado")
                    )
                    return@launch
                }

                val categoriaId = repository.crearCategoria(nombre, colorHex, orden)

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = tr("categoria_creada_ok"),
                    error = null
                )

                Log.d(TAG, "✅ Categoría creada: $nombre (ID: $categoriaId)")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = tr("error_creando_categoria_detalle", e.message ?: "")
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
                        error = tr("categoria_nombre_duplicado")
                    )
                    return@launch
                }

                val exito = repository.actualizarCategoria(categoria)

                if (exito) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        message = tr("categoria_actualizada_ok"),
                        error = null
                    )
                    Log.d(TAG, "✅ Categoría actualizada: ${categoria.nombre}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = tr("error_actualizando_categoria")
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = tr("error_actualizando_categoria_detalle", e.message ?: "")
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
                        message = tr("categoria_eliminada_ok"),
                        error = null
                    )
                    Log.d(TAG, "✅ Categoría eliminada: ${categoria.nombre}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = tr("error_eliminando_categoria")
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = tr("error_eliminando_categoria_detalle", e.message ?: "")
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
                    message = if (exito) tr("sincronizacion_completada") else tr("error_en_sincronizacion"),
                    error = if (!exito) tr("no_se_pudo_completar_sincronizacion") else null
                )

                Log.d(TAG, if (exito) "✅ Sincronización forzada exitosa" else "⚠️ Error en sincronización forzada")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = tr("error_en_sincronizacion_detalle", e.message ?: "")
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
            nombre.isBlank() -> tr("nombre_vacio")
            nombre.length < 2 -> tr("nombre_min_2")
            nombre.length > 50 -> tr("nombre_max_50")
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
            tr("color_invalido")
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
