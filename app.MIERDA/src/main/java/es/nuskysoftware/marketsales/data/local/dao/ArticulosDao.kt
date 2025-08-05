// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/ArticuloDao.kt
package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.ArticuloEntity
import kotlinx.coroutines.flow.Flow

/**
 * ArticuloDao V11 - Market Sales
 *
 * DIFERENCIAS CON CAJA MERCADILLOS:
 * - Todas las consultas filtradas por userId
 * - Arquitectura híbrida con sincronizadoFirebase
 * - Compatibilidad con sistema "Reloj Suizo"
 */
@Dao
interface ArticuloDao {

    // ========== OPERACIONES BÁSICAS ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ArticuloEntity>)

    @Query("DELETE FROM articulos WHERE userId = :userId")
    suspend fun borrarPorUsuario(userId: String)

    @Upsert
    suspend fun upsert(articulo: ArticuloEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM articulos WHERE userId = :uid LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticulo(articulo: ArticuloEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(articulo: ArticuloEntity)

    @Update
    suspend fun updateArticulo(articulo: ArticuloEntity)

    @Delete
    suspend fun deleteArticulo(articulo: ArticuloEntity)

    // ========== CONSULTAS FILTRADAS POR USUARIO ==========

    /**
     * Obtiene todos los artículos del usuario actual ordenados por nombre
     */
    @Query("SELECT * FROM articulos WHERE userId = :userId AND activo = 1 ORDER BY nombre ASC")
    fun getArticulosByUser(userId: String): Flow<List<ArticuloEntity>>

    /**
     * Obtiene artículos filtrados por categoría
     */
    @Query("SELECT * FROM articulos WHERE userId = :userId AND idCategoria = :categoriaId AND activo = 1 ORDER BY nombre ASC")
    fun getArticulosByUserAndCategoria(userId: String, categoriaId: String): Flow<List<ArticuloEntity>>

    /**
     * Obtiene un artículo específico por ID
     */
    @Query("SELECT * FROM articulos WHERE idArticulo = :id")
    suspend fun getArticuloById(id: String): ArticuloEntity?

    /**
     * Elimina artículo por ID
     */
    @Query("DELETE FROM articulos WHERE idArticulo = :id")
    suspend fun deleteArticuloById(id: String)

    // ========== SINCRONIZACIÓN HÍBRIDA ==========

    /**
     * Obtiene artículos no sincronizados de un usuario específico
     */
    @Query("SELECT * FROM articulos WHERE userId = :userId AND sincronizadoFirebase = 0")
    suspend fun getArticulosNoSincronizadosByUser(userId: String): List<ArticuloEntity>

    /**
     * Marca artículo como sincronizado
     */
    @Query("UPDATE articulos SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idArticulo = :id")
    suspend fun marcarComoSincronizado(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Marca artículo como no sincronizado (para cambios locales)
     */
    @Query("UPDATE articulos SET sincronizadoFirebase = 0 WHERE idArticulo = :id")
    suspend fun marcarComoNoSincronizado(id: String)

    /**
     * Obtiene la versión actual de un artículo
     */
    @Query("SELECT version FROM articulos WHERE idArticulo = :id")
    suspend fun getArticuloVersion(id: String): Long?

    // ========== UTILIDADES ==========

    /**
     * Cuenta el total de artículos de un usuario
     */
    @Query("SELECT COUNT(*) FROM articulos WHERE userId = :userId AND activo = 1")
    suspend fun getArticuloCountByUser(userId: String): Int

    /**
     * Verifica si existe un artículo con el mismo nombre para un usuario
     */
    @Query("SELECT COUNT(*) > 0 FROM articulos WHERE userId = :userId AND nombre = :nombre AND activo = 1 AND idArticulo != :excludeId")
    suspend fun existeArticuloConNombre(userId: String, nombre: String, excludeId: String = ""): Boolean

    /**
     * Obtiene nombres únicos de artículos para autocompletado
     */
    @Query("SELECT DISTINCT nombre FROM articulos WHERE userId = :userId AND activo = 1 ORDER BY nombre ASC")
    suspend fun getNombresArticulosUnicos(userId: String): List<String>

    /**
     * Busca artículos por nombre
     */
    @Query("SELECT * FROM articulos WHERE userId = :userId AND nombre LIKE '%' || :query || '%' AND activo = 1 ORDER BY nombre ASC")
    fun searchArticulosByNombre(userId: String, query: String): Flow<List<ArticuloEntity>>

    // ========== MANTENIMIENTO ==========

    /**
     * Elimina artículos inactivos antiguos
     */
    @Query("DELETE FROM articulos WHERE userId = :userId AND activo = 0 AND lastModified < :limiteFecha")
    suspend fun limpiarArticulosInactivosAntiguos(userId: String, limiteFecha: Long)

    /**
     * Resetea sincronización para forzar re-sync
     */
    @Query("UPDATE articulos SET sincronizadoFirebase = 0 WHERE userId = :userId")
    suspend fun resetearSincronizacion(userId: String)
}