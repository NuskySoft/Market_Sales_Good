// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/CategoriaDao.kt
package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Consultas y escrituras de categorías del catálogo por propietario.

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

/**
 * CategoriaDao V11 - Market Sales
 *
 * DIFERENCIAS CON CAJA MERCADILLOS:
 * - Todas las consultas filtradas por userId
 * - Arquitectura híbrida con sincronizadoFirebase
 * - Compatibilidad con sistema "Reloj Suizo"
 */
@Dao
interface CategoriaDao {
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM categorias WHERE userId = :uid AND idCategoria != 'uncategorized:' || :uid")
    suspend fun contarDemo(uid: String): Int


    // ========== OPERACIONES BÁSICAS ==========


    @Upsert
    suspend fun upsert(categoria: CategoriaEntity)

    // Consulta local de lectura; comprueba la existencia de registros del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT EXISTS(SELECT 1 FROM categorias WHERE userId = :uid LIMIT 1)")
    suspend fun existeAlgunoDeUsuario(uid: String): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoria(categoria: CategoriaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(categoria: CategoriaEntity)

    @Update
    suspend fun updateCategoria(categoria: CategoriaEntity)

    @Delete
    suspend fun deleteCategoria(categoria: CategoriaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CategoriaEntity>)

    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM categorias WHERE userId = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM categorias WHERE userId = :uid")
    suspend fun contarPorUsuario(uid: String): Int

    // ========== CONSULTAS FILTRADAS POR USUARIO ==========

    /**
     * Obtiene todas las categorías del usuario actual ordenadas por nombre
     */


    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM categorias WHERE userId = :userId AND activa = 1 ORDER BY nombre ASC")
    fun getCategoriasByUser(userId: String): Flow<List<CategoriaEntity>>

    /**
     * Obtiene una categoría específica por ID
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM categorias WHERE idCategoria = :id AND userId = :sessionOwner ")
    suspend fun getCategoriaById(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): CategoriaEntity?

    /**
     * Elimina categoría por ID
     */
    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM categorias WHERE idCategoria = :id AND userId = :sessionOwner ")
    suspend fun deleteCategoriaById(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    // ========== SINCRONIZACIÓN HÍBRIDA ==========

    /**
     * Obtiene categorías no sincronizadas de un usuario específico
     */
    // Consulta local de lectura del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM categorias WHERE userId = :userId AND sincronizadoFirebase = 0")
    suspend fun getCategoriasNoSincronizadasByUser(userId: String): List<CategoriaEntity>

    /**
     * Marca categoría como sincronizada
     */
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE categorias SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idCategoria = :id AND userId = :sessionOwner ")
    suspend fun marcarComoSincronizada(id: String, timestamp: Long = System.currentTimeMillis(), sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    /**
     * Marca categoría como no sincronizada (para cambios locales)
     */
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE categorias SET sincronizadoFirebase = 0 WHERE idCategoria = :id AND userId = :sessionOwner ")
    suspend fun marcarComoNoSincronizada(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty())

    /**
     * Obtiene la versión actual de una categoría
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT version FROM categorias WHERE idCategoria = :id AND userId = :sessionOwner ")
    suspend fun getCategoriaVersion(id: String, sessionOwner: String = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId().orEmpty()): Long?

    // ========== UTILIDADES ==========

    /**
     * Cuenta el total de categorías de un usuario
     */
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM categorias WHERE userId = :userId AND activa = 1")
    suspend fun getCategoriaCountByUser(userId: String): Int

    /**
     * Verifica si existe una categoría con el mismo nombre para un usuario
     */
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) > 0 FROM categorias WHERE userId = :userId AND nombre = :nombre AND activa = 1 AND idCategoria != :excludeId")
    suspend fun existeCategoriaConNombre(userId: String, nombre: String, excludeId: String = ""): Boolean

    /**
     * Obtiene nombres únicos de categorías para autocompletado
     */
    // Consulta local de lectura del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT DISTINCT nombre FROM categorias WHERE userId = :userId AND activa = 1 ORDER BY nombre ASC")
    suspend fun getNombresCategoriasUnicas(userId: String): List<String>

    // ========== MANTENIMIENTO ==========

    /**
     * Elimina categorías inactivas antiguas
     */
    // Eliminación local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("DELETE FROM categorias WHERE userId = :userId AND activa = 0 AND lastModified < :limiteFecha")
    suspend fun limpiarCategoriasInactivasAntiguas(userId: String, limiteFecha: Long)

    /**
     * Resetea sincronización para forzar re-sync
     */
    // Actualización local de filas del propietario indicado según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE categorias SET sincronizadoFirebase = 0 WHERE userId = :userId")
    suspend fun resetearSincronizacion(userId: String)
}
