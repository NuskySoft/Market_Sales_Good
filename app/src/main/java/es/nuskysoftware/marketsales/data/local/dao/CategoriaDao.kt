// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/CategoriaDao.kt
package es.nuskysoftware.marketsales.data.local.dao

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

    // ========== OPERACIONES BÁSICAS ==========


    @Upsert
    suspend fun upsert(categoria: CategoriaEntity)

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

    @Query("DELETE FROM categorias WHERE userId = :userId")
    suspend fun borrarPorUsuario(userId: String)

    // ========== CONSULTAS FILTRADAS POR USUARIO ==========

    /**
     * Obtiene todas las categorías del usuario actual ordenadas por nombre
     */


    @Query("SELECT * FROM categorias WHERE userId = :userId AND activa = 1 ORDER BY nombre ASC")
    fun getCategoriasByUser(userId: String): Flow<List<CategoriaEntity>>

    /**
     * Obtiene una categoría específica por ID
     */
    @Query("SELECT * FROM categorias WHERE idCategoria = :id")
    suspend fun getCategoriaById(id: String): CategoriaEntity?

    /**
     * Elimina categoría por ID
     */
    @Query("DELETE FROM categorias WHERE idCategoria = :id")
    suspend fun deleteCategoriaById(id: String)

    // ========== SINCRONIZACIÓN HÍBRIDA ==========

    /**
     * Obtiene categorías no sincronizadas de un usuario específico
     */
    @Query("SELECT * FROM categorias WHERE userId = :userId AND sincronizadoFirebase = 0")
    suspend fun getCategoriasNoSincronizadasByUser(userId: String): List<CategoriaEntity>

    /**
     * Marca categoría como sincronizada
     */
    @Query("UPDATE categorias SET sincronizadoFirebase = 1, lastModified = :timestamp WHERE idCategoria = :id")
    suspend fun marcarComoSincronizada(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Marca categoría como no sincronizada (para cambios locales)
     */
    @Query("UPDATE categorias SET sincronizadoFirebase = 0 WHERE idCategoria = :id")
    suspend fun marcarComoNoSincronizada(id: String)

    /**
     * Obtiene la versión actual de una categoría
     */
    @Query("SELECT version FROM categorias WHERE idCategoria = :id")
    suspend fun getCategoriaVersion(id: String): Long?

    // ========== UTILIDADES ==========

    /**
     * Cuenta el total de categorías de un usuario
     */
    @Query("SELECT COUNT(*) FROM categorias WHERE userId = :userId AND activa = 1")
    suspend fun getCategoriaCountByUser(userId: String): Int

    /**
     * Verifica si existe una categoría con el mismo nombre para un usuario
     */
    @Query("SELECT COUNT(*) > 0 FROM categorias WHERE userId = :userId AND nombre = :nombre AND activa = 1 AND idCategoria != :excludeId")
    suspend fun existeCategoriaConNombre(userId: String, nombre: String, excludeId: String = ""): Boolean

    /**
     * Obtiene nombres únicos de categorías para autocompletado
     */
    @Query("SELECT DISTINCT nombre FROM categorias WHERE userId = :userId AND activa = 1 ORDER BY nombre ASC")
    suspend fun getNombresCategoriasUnicas(userId: String): List<String>

    // ========== MANTENIMIENTO ==========

    /**
     * Elimina categorías inactivas antiguas
     */
    @Query("DELETE FROM categorias WHERE userId = :userId AND activa = 0 AND lastModified < :limiteFecha")
    suspend fun limpiarCategoriasInactivasAntiguas(userId: String, limiteFecha: Long)

    /**
     * Resetea sincronización para forzar re-sync
     */
    @Query("UPDATE categorias SET sincronizadoFirebase = 0 WHERE userId = :userId")
    suspend fun resetearSincronizacion(userId: String)
}