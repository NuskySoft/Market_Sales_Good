// app/src/main/java/es/nuskysoftware/marketsales/data/repository/CategoriaRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.CategoriaDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * CategoriaRepository V11 - Market Sales
 *
 * ARQUITECTURA HÍBRIDA "RELOJ SUIZO":
 * - Local First: Toda operación se ejecuta primero en Room
 * - Sync When Possible: Sincronización automática cuando hay conexión
 * - Never Block UI: La interfaz nunca espera operaciones de red
 * - Graceful Degradation: Funcionamiento completo offline
 * - Filtrado por usuario: Cada usuario solo ve sus datos
 */
class CategoriaRepository(
    context: Context
) {
    private val categoriaDao: CategoriaDao = AppDatabase.getDatabase(context).categoriaDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val connectivityObserver = ConnectivityObserver(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "CategoriaRepository"
    }

    init {
        // Sincronización automática cuando volvemos online
        repositoryScope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    val userId = ConfigurationManager.getCurrentUserId()
                    if (userId != null) {
                        sincronizarCategoriasNoSincronizadas(userId)
                    }
                }
            }
        }
    }

    // ========== OPERACIONES PRINCIPALES ==========

    /**
     * Obtiene todas las categorías del usuario actual
     */
    fun getCategoriasUsuarioActual(): Flow<List<CategoriaEntity>> {
        val userId = ConfigurationManager.getCurrentUserId()
        return if (userId != null) {
            categoriaDao.getCategoriasByUser(userId)
        } else {
            flowOf(emptyList())
        }
    }

    /**
     * Crea una nueva categoría - PATRÓN HÍBRIDO
     */
    suspend fun crearCategoria(
        nombre: String,
        colorHex: String,
        orden: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId()
            ?: throw IllegalStateException("No se puede crear categoría sin usuario")

        Log.d(TAG, "🔄 Creando categoría para usuario: $userId")

        val nuevaCategoria = CategoriaEntity(
            userId = userId,
            nombre = nombre,
            colorHex = colorHex,
            orden = orden,
            sincronizadoFirebase = false
        )

        try {
            // 1. GUARDAR EN ROOM PRIMERO (respuesta inmediata)
            categoriaDao.insertCategoria(nuevaCategoria)
            Log.d(TAG, "✅ Categoría guardada en Room: ${nuevaCategoria.nombre}")

            // 2. INTENTAR SINCRONIZAR CON FIREBASE
            sincronizarCategoriaConFirebase(nuevaCategoria)

            return@withContext nuevaCategoria.idCategoria

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando categoría", e)
            throw e
        }
    }

    /**
     * Actualiza una categoría existente - PATRÓN HÍBRIDO
     */
    suspend fun actualizarCategoria(categoria: CategoriaEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val categoriaActualizada = categoria.copy(
                version = categoria.version + 1,
                lastModified = System.currentTimeMillis(),
                sincronizadoFirebase = false
            )

            // 1. ACTUALIZAR EN ROOM PRIMERO
            categoriaDao.updateCategoria(categoriaActualizada)
            Log.d(TAG, "✅ Categoría actualizada en Room: ${categoriaActualizada.nombre}")

            // 2. INTENTAR SINCRONIZAR CON FIREBASE
            sincronizarCategoriaConFirebase(categoriaActualizada)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando categoría", e)
            false
        }
    }

    /**
     * Elimina una categoría - PATRÓN HÍBRIDO
     */
    suspend fun eliminarCategoria(categoria: CategoriaEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. ELIMINAR DE ROOM PRIMERO
            categoriaDao.deleteCategoria(categoria)
            Log.d(TAG, "✅ Categoría eliminada de Room: ${categoria.nombre}")

            // 2. INTENTAR ELIMINAR DE FIREBASE
            eliminarCategoriaDeFirebase(categoria.idCategoria)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando categoría", e)
            false
        }
    }

    /**
     * Obtiene categoría por ID
     */
    suspend fun getCategoriaById(id: String): CategoriaEntity? = withContext(Dispatchers.IO) {
        categoriaDao.getCategoriaById(id)
    }

    // ========== ESTRATEGIA HÍBRIDA INTELIGENTE ==========

    /**
     * Obtiene datos híbridos: combina lo mejor de Room y Firebase
     */
    suspend fun getHybridCategorias(userId: String): List<CategoriaEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Verificar cambios pendientes en Room
            val categoriasNoSincronizadas = categoriaDao.getCategoriasNoSincronizadasByUser(userId)

            if (categoriasNoSincronizadas.isNotEmpty()) {
                // HAY CAMBIOS PENDIENTES → Room es fuente de verdad
                Log.d(TAG, "📱 Usando Room (${categoriasNoSincronizadas.size} cambios pendientes)")
                return@withContext categoriaDao.getCategoriasByUser(userId).first()
            } else {
                // NO HAY CAMBIOS PENDIENTES → Intentar Firebase
                Log.d(TAG, "☁️ Intentando Firebase (sin cambios pendientes)")

                if (connectivityObserver.isConnected.first()) {
                    try {
                        val firebaseCategorias = descargarCategoriasDesdeFirebase(userId)
                        if (firebaseCategorias.isNotEmpty()) {
                            // Actualizar Room con datos frescos
                            firebaseCategorias.forEach { categoria ->
                                categoriaDao.insertOrUpdate(categoria.copy(sincronizadoFirebase = true))
                            }
                            Log.d(TAG, "✅ Datos frescos de Firebase aplicados")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Error con Firebase, usando Room como fallback", e)
                    }
                }

                // Devolver datos de Room (actualizados o fallback)
                return@withContext categoriaDao.getCategoriasByUser(userId).first()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en estrategia híbrida", e)
            // Fallback total a Room
            return@withContext categoriaDao.getCategoriasByUser(userId).first()
        }
    }

    // ========== SINCRONIZACIÓN CON FIREBASE ==========

    /**
     * Sincroniza una categoría específica con Firebase
     */
    private suspend fun sincronizarCategoriaConFirebase(categoria: CategoriaEntity) {
        try {
            if (!connectivityObserver.isConnected.first()) {
                Log.d(TAG, "⚠️ Sin conexión, categría quedará pendiente de sincronización")
                return
            }

            val datos = mapOf(
                "idCategoria" to categoria.idCategoria,
                "userId" to categoria.userId,
                "nombre" to categoria.nombre,
                "colorHex" to categoria.colorHex,
                "orden" to categoria.orden,
                "activa" to categoria.activa,
                "version" to categoria.version,
                "lastModified" to categoria.lastModified,
                "fechaSync" to System.currentTimeMillis()
            )

            firestore.collection("categorias")
                .document(categoria.idCategoria)
                .set(datos)
                .await()

            // Marcar como sincronizada en Room
            categoriaDao.marcarComoSincronizada(categoria.idCategoria)
            Log.d(TAG, "☁️ Categoría sincronizada con Firebase: ${categoria.nombre}")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error sincronizando con Firebase: ${categoria.nombre}", e)
            // La categoría queda marcada como no sincronizada para reintento posterior
        }
    }

    /**
     * Elimina categoría de Firebase
     */
    private suspend fun eliminarCategoriaDeFirebase(categoriaId: String) {
        try {
            if (connectivityObserver.isConnected.first()) {
                firestore.collection("categorias")
                    .document(categoriaId)
                    .delete()
                    .await()
                Log.d(TAG, "☁️ Categoría eliminada de Firebase: $categoriaId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error eliminando de Firebase: $categoriaId", e)
        }
    }

    /**
     * Sincroniza categorías no sincronizadas cuando hay conexión
     */
    private suspend fun sincronizarCategoriasNoSincronizadas(userId: String) {
        try {
            val pendientes = categoriaDao.getCategoriasNoSincronizadasByUser(userId)
            Log.d(TAG, "🔄 Sincronizando ${pendientes.size} categorías pendientes")

            pendientes.forEach { categoria ->
                sincronizarCategoriaConFirebase(categoria)
                delay(100) // Evitar saturar Firebase
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando pendientes", e)
        }
    }

    /**
     * Descarga categorías desde Firebase
     */
    private suspend fun descargarCategoriasDesdeFirebase(userId: String): List<CategoriaEntity> {
        return try {
            val snapshot = firestore.collection("categorias")
                .whereEqualTo("userId", userId)
                .whereEqualTo("activa", true)
                .get()
                .await()

            val categorias = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    CategoriaEntity(
                        idCategoria = data["idCategoria"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        nombre = data["nombre"] as? String ?: "",
                        colorHex = data["colorHex"] as? String ?: "#FFFFFF",
                        orden = (data["orden"] as? Long)?.toInt() ?: 0,
                        activa = data["activa"] as? Boolean ?: true,
                        version = data["version"] as? Long ?: 1L,
                        lastModified = data["lastModified"] as? Long ?: System.currentTimeMillis(),
                        sincronizadoFirebase = true
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error parseando categoría de Firebase", e)
                    null
                }
            }

            Log.d(TAG, "☁️ Descargadas ${categorias.size} categorías de Firebase")
            categorias

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando de Firebase", e)
            emptyList()
        }
    }

    // ========== UTILIDADES ==========

    /**
     * Verifica si existe una categoría con el mismo nombre
     */
    suspend fun existeCategoriaConNombre(nombre: String, excludeId: String = ""): Boolean {
        val userId = ConfigurationManager.getCurrentUserId() ?: return false
        return categoriaDao.existeCategoriaConNombre(userId, nombre, excludeId)
    }

    /**
     * Fuerza sincronización completa
     */
    suspend fun forzarSincronizacion(): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = ConfigurationManager.getCurrentUserId() ?: return@withContext false

            sincronizarCategoriasNoSincronizadas(userId)
            getHybridCategorias(userId) // Esto descargará datos frescos de Firebase

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en sincronización forzada", e)
            false
        }
    }
}