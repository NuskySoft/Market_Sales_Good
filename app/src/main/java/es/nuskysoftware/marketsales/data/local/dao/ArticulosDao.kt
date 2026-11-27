// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/ArticuloDao.kt
package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Consultas y escrituras de artículos, precios, existencias y cambios pendientes.

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
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM articulos WHERE userId = :uid")
    suspend fun contarDemo(uid: String): Int


    // ========== OPERACIONES BÁSICAS ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ArticuloEntity>)

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM articulos WHERE userId = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM categorias WHERE userId = :uid")
    suspend fun contarPorUsuario(uid: String): Int

    @Upsert
    suspend fun upsert(articulo: ArticuloEntity)

    // Consulta local de lectura; comprueba la existencia de registros del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
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
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM articulos WHERE userId = :userId AND activo = 1 ORDER BY nombre ASC")
    fun getArticulosByUser(userId: String): Flow<List<ArticuloEntity>>

    /**
     * Obtiene artículos filtrados por categoría
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM articulos WHERE userId = :userId AND idCategoria = :categoriaId AND activo = 1 ORDER BY nombre ASC")
    fun getArticulosByUserAndCategoria(userId: String, categoriaId: String): Flow<List<ArticuloEntity>>

    /**
     * Obtiene un artículo específico por ID
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM articulos WHERE idArticulo = :id AND userId = :sessionOwner ")
    suspend fun getArticuloById(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): ArticuloEntity?

    /**
     * Elimina artículo por ID
     */
    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM articulos WHERE idArticulo = :id AND userId = :sessionOwner ")
    suspend fun deleteArticuloById(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // ========== SINCRONIZACIÓN HÍBRIDA ==========

    /**
     * Obtiene artículos no sincronizados de un usuario específico
     */
    // Consulta local de lectura del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM articulos WHERE userId = :userId AND sincronizadoFirebase = 0")
    suspend fun getArticulosNoSincronizadosByUser(userId: String): List<ArticuloEntity>

    /**
     * Marca artículo como sincronizado
     */
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE articulos SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idArticulo = :id AND userId = :sessionOwner ")
    suspend fun marcarComoSincronizado(id: String, timestamp: Long = System.currentTimeMillis(), sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    /**
     * Marca artículo como no sincronizado (para cambios locales)
     */
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE articulos SET sincronizadoFirebase = 0 WHERE idArticulo = :id AND userId = :sessionOwner ")
    suspend fun marcarComoNoSincronizado(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    /**
     * Obtiene la versión actual de un artículo
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT version FROM articulos WHERE idArticulo = :id AND userId = :sessionOwner ")
    suspend fun getArticuloVersion(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Long?

    // A stock adjustment during upload must remain dirty for another upload.
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE articulos SET sincronizadoFirebase = 1 WHERE idArticulo = :id AND userId = :owner AND version = :version AND lastModified = :modified AND stock IS :stock")
    suspend fun marcarSnapshotSincronizado(id: String, owner: String, version: Long, modified: Long, stock: Int?)

    // ========== UTILIDADES ==========

    /**
     * Cuenta el total de artículos de un usuario
     */
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM articulos WHERE userId = :userId AND activo = 1")
    suspend fun getArticuloCountByUser(userId: String): Int

    /**
     * Verifica si existe un artículo con el mismo nombre para un usuario
     */
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) > 0 FROM articulos WHERE userId = :userId AND nombre = :nombre AND activo = 1 AND idArticulo != :excludeId")
    suspend fun existeArticuloConNombre(userId: String, nombre: String, excludeId: String = ""): Boolean

    /**
     * Obtiene nombres únicos de artículos para autocompletado
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT DISTINCT nombre FROM articulos WHERE userId = :userId AND activo = 1 ORDER BY nombre ASC")
    suspend fun getNombresArticulosUnicos(userId: String): List<String>

    /**
     * Busca artículos por nombre
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM articulos WHERE userId = :userId AND nombre LIKE '%' || :query || '%' AND activo = 1 ORDER BY nombre ASC")
    fun searchArticulosByNombre(userId: String, query: String): Flow<List<ArticuloEntity>>

    // ========== MANTENIMIENTO ==========

    /**
     * Elimina artículos inactivos antiguos
     */
    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM articulos WHERE userId = :userId AND activo = 0 AND lastModified < :limiteFecha")
    suspend fun limpiarArticulosInactivosAntiguos(userId: String, limiteFecha: Long)

    /**
     * Resetea sincronización para forzar re-sync
     */
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE articulos SET sincronizadoFirebase = 0 WHERE userId = :userId")
    suspend fun resetearSincronizacion(userId: String)
}
