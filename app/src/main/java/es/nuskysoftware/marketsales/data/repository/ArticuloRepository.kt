// app/src/main/java/es/nuskysoftware/marketsales/data/repository/ArticuloRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.ArticuloDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.ArticuloEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * ArticuloRepository V11 - Market Sales
 *
 * ARQUITECTURA HÍBRIDA "RELOJ SUIZO":
 * - Local First: Toda operación se ejecuta primero en Room
 * - Sync When Possible: Sincronización automática cuando hay conexión
 * - Never Block UI: La interfaz nunca espera operaciones de red
 * - Graceful Degradation: Funcionamiento completo offline
 * - Filtrado por usuario: Cada usuario solo ve sus datos
 */
class ArticuloRepository(
    context: Context
) {
    private val articuloDao: ArticuloDao = AppDatabase.getDatabase(context).articuloDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val connectivityObserver = ConnectivityObserver(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ArticuloRepository"
    }

    init {
        // Sincronización automática cuando volvemos online
        repositoryScope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    val userId = ConfigurationManager.getCurrentUserId()
                    if (userId != null) {
                        sincronizarArticulosNoSincronizados(userId)
                    }
                }
            }
        }
    }

    // ========== OPERACIONES PRINCIPALES ==========

    /**
     * Obtiene todos los artículos del usuario actual
     */
    fun getArticulosUsuarioActual(): Flow<List<ArticuloEntity>> {
        val userId = ConfigurationManager.getCurrentUserId()
        return if (userId != null) {
            articuloDao.getArticulosByUser(userId)
        } else {
            flowOf(emptyList())
        }
    }

    /**
     * Obtiene artículos filtrados por categoría
     */
    fun getArticulosByCategoria(categoriaId: String): Flow<List<ArticuloEntity>> {
        val userId = ConfigurationManager.getCurrentUserId()
        return if (userId != null) {
            articuloDao.getArticulosByUserAndCategoria(userId, categoriaId)
        } else {
            flowOf(emptyList())
        }
    }

    /**
     * Crea un nuevo artículo - PATRÓN HÍBRIDO
     */
    suspend fun crearArticulo(
        nombre: String,
        idCategoria: String,
        precioVenta: Double,
        precioCoste: Double? = null,
        stock: Int? = null,
        controlarStock: Boolean = false,
        controlarCoste: Boolean = false,
        favorito: Boolean = false,
        fotoUri: String? = null
    ): String = withContext(Dispatchers.IO) {
        val userId = ConfigurationManager.getCurrentUserId()
            ?: throw IllegalStateException("No se puede crear artículo sin usuario")

        Log.d(TAG, "🔄 Creando artículo para usuario: $userId")

        val nuevoArticulo = ArticuloEntity(
            userId = userId,
            nombre = nombre,
            idCategoria = idCategoria,
            precioVenta = precioVenta,
            precioCoste = precioCoste,
            stock = stock,
            controlarStock = controlarStock,
            controlarCoste = controlarCoste,
            favorito = favorito,
            fotoUri = fotoUri,
            sincronizadoFirebase = false
        )

        try {
            // 1. GUARDAR EN ROOM PRIMERO (respuesta inmediata)
            articuloDao.insertArticulo(nuevoArticulo)
            Log.d(TAG, "✅ Artículo guardado en Room: ${nuevoArticulo.nombre}")

            // 2. INTENTAR SINCRONIZAR CON FIREBASE
            sincronizarArticuloConFirebase(nuevoArticulo)

            return@withContext nuevoArticulo.idArticulo

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando artículo", e)
            throw e
        }
    }

    /**
     * Actualiza un artículo existente - PATRÓN HÍBRIDO
     */
    suspend fun actualizarArticulo(articulo: ArticuloEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val articuloActualizado = articulo.copy(
                version = articulo.version + 1,
                lastModified = System.currentTimeMillis(),
                sincronizadoFirebase = false
            )

            // 1. ACTUALIZAR EN ROOM PRIMERO
            articuloDao.updateArticulo(articuloActualizado)
            Log.d(TAG, "✅ Artículo actualizado en Room: ${articuloActualizado.nombre}")

            // 2. INTENTAR SINCRONIZAR CON FIREBASE
            sincronizarArticuloConFirebase(articuloActualizado)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando artículo", e)
            false
        }
    }

    /**
     * Elimina un artículo - PATRÓN HÍBRIDO
     */
    suspend fun eliminarArticulo(articulo: ArticuloEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. ELIMINAR DE ROOM PRIMERO
            articuloDao.deleteArticulo(articulo)
            Log.d(TAG, "✅ Artículo eliminado de Room: ${articulo.nombre}")

            // 2. INTENTAR ELIMINAR DE FIREBASE
            eliminarArticuloDeFirebase(articulo.idArticulo)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando artículo", e)
            false
        }
    }

    /**
     * Obtiene artículo por ID
     */
    suspend fun getArticuloById(id: String): ArticuloEntity? = withContext(Dispatchers.IO) {
        articuloDao.getArticuloById(id)
    }

    // ========== ESTRATEGIA HÍBRIDA INTELIGENTE ==========

    /**
     * Obtiene datos híbridos: combina lo mejor de Room y Firebase
     */
    suspend fun getHybridArticulos(userId: String): List<ArticuloEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Verificar cambios pendientes en Room
            val articulosNoSincronizados = articuloDao.getArticulosNoSincronizadosByUser(userId)

            if (articulosNoSincronizados.isNotEmpty()) {
                // HAY CAMBIOS PENDIENTES → Room es fuente de verdad
                Log.d(TAG, "📱 Usando Room (${articulosNoSincronizados.size} cambios pendientes)")
                return@withContext articuloDao.getArticulosByUser(userId).first()
            } else {
                // NO HAY CAMBIOS PENDIENTES → Intentar Firebase
                Log.d(TAG, "☁️ Intentando Firebase (sin cambios pendientes)")

                if (connectivityObserver.isConnected.first()) {
                    try {
                        val firebaseArticulos = descargarArticulosDesdeFirebase(userId)
                        if (firebaseArticulos.isNotEmpty()) {
                            // Actualizar Room con datos frescos
                            firebaseArticulos.forEach { articulo ->
                                articuloDao.insertOrUpdate(articulo.copy(sincronizadoFirebase = true))
                            }
                            Log.d(TAG, "✅ Datos frescos de Firebase aplicados")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Error con Firebase, usando Room como fallback", e)
                    }
                }

                // Devolver datos de Room (actualizados o fallback)
                return@withContext articuloDao.getArticulosByUser(userId).first()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en estrategia híbrida", e)
            // Fallback total a Room
            return@withContext articuloDao.getArticulosByUser(userId).first()
        }
    }

    // ========== SINCRONIZACIÓN CON FIREBASE ==========

    /**
     * Sincroniza un artículo específico con Firebase
     */
    private suspend fun sincronizarArticuloConFirebase(articulo: ArticuloEntity) {
        try {
            if (!connectivityObserver.isConnected.first()) {
                Log.d(TAG, "⚠️ Sin conexión, artículo quedará pendiente de sincronización")
                return
            }

            val datos = mapOf(
                "idArticulo" to articulo.idArticulo,
                "userId" to articulo.userId,
                "nombre" to articulo.nombre,
                "idCategoria" to articulo.idCategoria,
                "precioVenta" to articulo.precioVenta,
                "precioCoste" to articulo.precioCoste,
                "stock" to articulo.stock,
                "controlarStock" to articulo.controlarStock,
                "controlarCoste" to articulo.controlarCoste,
                "favorito" to articulo.favorito,
                "fotoUri" to articulo.fotoUri,
                "activo" to articulo.activo,
                "version" to articulo.version,
                "lastModified" to articulo.lastModified,
                "fechaSync" to System.currentTimeMillis()
            )

            firestore.collection("articulos")
                .document(articulo.idArticulo)
                .set(datos)
                .await()

            // Marcar como sincronizado en Room
            articuloDao.marcarComoSincronizado(articulo.idArticulo)
            Log.d(TAG, "☁️ Artículo sincronizado con Firebase: ${articulo.nombre}")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error sincronizando con Firebase: ${articulo.nombre}", e)
            // El artículo queda marcado como no sincronizado para reintento posterior
        }
    }

    /**
     * Elimina artículo de Firebase
     */
    private suspend fun eliminarArticuloDeFirebase(articuloId: String) {
        try {
            if (connectivityObserver.isConnected.first()) {
                firestore.collection("articulos")
                    .document(articuloId)
                    .delete()
                    .await()
                Log.d(TAG, "☁️ Artículo eliminado de Firebase: $articuloId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error eliminando de Firebase: $articuloId", e)
        }
    }

    /**
     * Sincroniza artículos no sincronizados cuando hay conexión
     */
    private suspend fun sincronizarArticulosNoSincronizados(userId: String) {
        try {
            val pendientes = articuloDao.getArticulosNoSincronizadosByUser(userId)
            Log.d(TAG, "🔄 Sincronizando ${pendientes.size} artículos pendientes")

            pendientes.forEach { articulo ->
                sincronizarArticuloConFirebase(articulo)
                delay(100) // Evitar saturar Firebase
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando pendientes", e)
        }
    }

    /**
     * Descarga artículos desde Firebase
     */
    private suspend fun descargarArticulosDesdeFirebase(userId: String): List<ArticuloEntity> {
        return try {
            val snapshot = firestore.collection("articulos")
                .whereEqualTo("userId", userId)
                .whereEqualTo("activo", true)
                .get()
                .await()

            val articulos = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    ArticuloEntity(
                        idArticulo = data["idArticulo"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        nombre = data["nombre"] as? String ?: "",
                        idCategoria = data["idCategoria"] as? String ?: "",
                        precioVenta = (data["precioVenta"] as? Number)?.toDouble() ?: 0.0,
                        precioCoste = (data["precioCoste"] as? Number)?.toDouble(),
                        stock = (data["stock"] as? Long)?.toInt(),
                        controlarStock = data["controlarStock"] as? Boolean ?: false,
                        controlarCoste = data["controlarCoste"] as? Boolean ?: false,
                        favorito = data["favorito"] as? Boolean ?: false,
                        fotoUri = data["fotoUri"] as? String,
                        activo = data["activo"] as? Boolean ?: true,
                        version = data["version"] as? Long ?: 1L,
                        lastModified = data["lastModified"] as? Long ?: System.currentTimeMillis(),
                        sincronizadoFirebase = true
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error parseando artículo de Firebase", e)
                    null
                }
            }

            Log.d(TAG, "☁️ Descargados ${articulos.size} artículos de Firebase")
            articulos

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando de Firebase", e)
            emptyList()
        }
    }

    // ========== UTILIDADES ==========

    /**
     * Verifica si existe un artículo con el mismo nombre
     */
    suspend fun existeArticuloConNombre(nombre: String, excludeId: String = ""): Boolean {
        val userId = ConfigurationManager.getCurrentUserId() ?: return false
        return articuloDao.existeArticuloConNombre(userId, nombre, excludeId)
    }

    /**
     * Busca artículos por nombre
     */
    fun searchArticulos(query: String): Flow<List<ArticuloEntity>> {
        val userId = ConfigurationManager.getCurrentUserId()
        return if (userId != null && query.isNotBlank()) {
            articuloDao.searchArticulosByNombre(userId, query)
        } else {
            flowOf(emptyList())
        }
    }

    /**
     * Fuerza sincronización completa
     */
    suspend fun forzarSincronizacion(): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = ConfigurationManager.getCurrentUserId() ?: return@withContext false

            sincronizarArticulosNoSincronizados(userId)
            getHybridArticulos(userId) // Esto descargará datos frescos de Firebase

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en sincronización forzada", e)
            false
        }
    }
}