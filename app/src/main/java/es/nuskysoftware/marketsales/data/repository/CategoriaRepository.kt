// app/src/main/java/es/nuskysoftware/marketsales/data/repository/CategoriaRepository.kt
package es.nuskysoftware.marketsales.data.repository
import es.nuskysoftware.marketsales.utils.DemoAccess


import androidx.room.withTransaction
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.CategoriaDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flatMapLatest
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
    context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val appContext = context.applicationContext
    private val categoriaDao: CategoriaDao = database.categoriaDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val connectivityObserver = ConnectivityObserver(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val localDb = database

    companion object {
        private const val TAG = "CategoriaRepository"
    }

    init {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    // ========== OPERACIONES PRINCIPALES ==========

    /**
     * Obtiene todas las categorías del usuario actual
     */
    fun getCategoriasUsuarioActual(): Flow<List<CategoriaEntity>> {
        return ConfigurationManager.usuarioLogueado.flatMapLatest { uid ->
            val owner = uid ?: es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID
            categoriaDao.getCategoriasByUser(owner)
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
            ?: throw IllegalStateException(es.nuskysoftware.marketsales.utils.StringResourceManager.getString("sin_usuario_categoria", ConfigurationManager.idioma.value))

        RecoveryGate(appContext).beforeWrite(userId)
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
            localDb.withTransaction {
                DemoAccess.requireOwner(userId)
                CatalogDefaults.ensure(database, userId)
                DemoAccess.checkLimit("categorias", categoriaDao.contarDemo(userId))
                categoriaDao.insertCategoria(nuevaCategoria)
            }
            Log.d(TAG, "✅ Categoría guardada en Room: ${nuevaCategoria.nombre}")

            // 2. INTENTAR SINCRONIZAR CON FIREBASE
            repositoryScope.launch { sincronizarCategoriaConFirebase(nuevaCategoria) }

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
            DemoAccess.requireOwner(categoria.userId)
            val categoriaActualizada = categoria.copy(
                version = categoria.version + 1,
                lastModified = System.currentTimeMillis(),
                sincronizadoFirebase = false
            )

            // 1. ACTUALIZAR EN ROOM PRIMERO
            categoriaDao.updateCategoria(categoriaActualizada)
            Log.d(TAG, "✅ Categoría actualizada en Room: ${categoriaActualizada.nombre}")

            // 2. INTENTAR SINCRONIZAR CON FIREBASE
            repositoryScope.launch { sincronizarCategoriaConFirebase(categoriaActualizada) }

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
            DemoAccess.requireOwner(categoria.userId)
            check(!CatalogDefaults.isProtected(categoria)) { "Protected category" }
            database.withTransaction {
                val target = CatalogDefaults.ensure(database, categoria.userId)
                val articles = database.articuloDao().getArticulosByUserAndCategoria(categoria.userId, categoria.idCategoria).first()
                for (a in articles) database.articuloDao().updateArticulo(a.copy(idCategoria = target, version = a.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
                categoriaDao.updateCategoria(categoria.copy(activa = false, version = categoria.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
            }
            es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
            true
        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { false }
    }

    /**
     * Obtiene categoría por ID
     */
    suspend fun getCategoriaById(id: String): CategoriaEntity? = withContext(Dispatchers.IO) {
        categoriaDao.getCategoriaById(id)?.takeIf { it.userId == ConfigurationManager.getCurrentUserId() }
    }

    // ========== ESTRATEGIA HÍBRIDA INTELIGENTE ==========

    /**
     * Obtiene datos híbridos: combina lo mejor de Room y Firebase
     */
    suspend fun getHybridCategorias(userId: String): List<CategoriaEntity> = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(userId)
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
        categoriaDao.getCategoriasByUser(userId).first()
    }

    // ========== SINCRONIZACIÓN CON FIREBASE ==========

    /**
     * Sincroniza una categoría específica con Firebase
     */
    private suspend fun sincronizarCategoriaConFirebase(categoria: CategoriaEntity) {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    /**
     * Elimina categoría de Firebase
     */
    private suspend fun eliminarCategoriaDeFirebase(categoriaId: String) {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
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
        if (!DemoAccess.canSync(userId)) return emptyList()
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
