// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/UserDao.kt
package es.nuskysoftware.marketsales.data.local.dao

// Responsabilidad: Persistencia local de perfiles de cuenta y estado de suscripción.

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import es.nuskysoftware.marketsales.data.local.entity.UserEntity

/**
 * ✅ UserDao COMPLETO con todos los métodos que necesita UserRepository
 */
@Dao
interface UserDao {

    // ========== MÉTODOS BÁSICOS ==========
    // ¿Existe el usuario en Room?
    // Consulta local de lectura; comprueba la existencia de registros. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT EXISTS(SELECT 1 FROM usuarios WHERE uid = :uid LIMIT 1)")
    suspend fun existeUsuario(uid: String): Boolean

    // ¿Es Premium el usuario? (null si no existe)
    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT esPremium FROM usuarios WHERE uid = :uid")
    suspend fun isPremium(uid: String): Boolean?

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    suspend fun getUserByIdSync(uid: String): UserEntity?

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    fun getUserByIdFlow(uid: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    // ========== MÉTODOS FALTANTES PARA UserRepository ==========

    /**
     * Obtener usuario actual (dummy - UserRepository lo maneja)
     */
    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE uid = (SELECT usuarioLogueado FROM configuracion WHERE id = 1)")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    /**
     * Obtener usuarios pendientes de sincronización
     */
    // Consulta local de lectura según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE sincronizadoFirebase = 0")
    suspend fun getUsersPendingSync(): List<UserEntity>

    /**
     * Actualizar estado Premium del usuario
     */
    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET esPremium = :esPremium WHERE uid = :uid")
    suspend fun updateUserPremium(uid: String, esPremium: Boolean)

    /**
     * Actualizar email del usuario
     */
    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET email = :email WHERE uid = :uid")
    suspend fun updateUserEmail(uid: String, email: String)

    /**
     * Actualizar nombre del usuario
     */
    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET displayName = :displayName WHERE uid = :uid")
    suspend fun updateUserDisplayName(uid: String, displayName: String)

    /**
     * Actualizar foto del usuario
     */
    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET photoUrl = :photoUrl WHERE uid = :uid")
    suspend fun updateUserPhotoUrl(uid: String, photoUrl: String)

    /**
     * Obtener todos los usuarios Premium
     */
    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE esPremium = 1 AND activo = 1")
    suspend fun getAllPremiumUsers(): List<UserEntity>

    /**
     * Obtener todos los usuarios Free
     */
    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE esPremium = 0 AND activo = 1")
    suspend fun getAllFreeUsers(): List<UserEntity>

    /**
     * Marcar usuario como sincronizado exitosamente
     */
    // Actualización local de filas según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET sincronizadoFirebase = 1, version = :newVersion WHERE uid = :uid")
    suspend fun markUserSyncSuccessful(uid: String, newVersion: Long)

    /**
     * Actualizar fecha de última sincronización
     */
    // Actualización local de filas. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET fechaUltimaSync = :fecha WHERE uid = :uid")
    suspend fun updateFechaUltimaSync(uid: String, fecha: String)

    /**
     * Obtener versión de usuario
     */
    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT version FROM usuarios WHERE uid = :uid")
    suspend fun getUserVersion(uid: String): Long?

    // ========== MÉTODOS EXISTENTES ==========
    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Consulta local de lectura. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT * FROM usuarios WHERE activo = 1")
    suspend fun getAllActiveUsers(): List<UserEntity>

    // En UserDao.kt - Alternativa más robusta:

    // Actualización local de filas del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET displayName = :displayName, email = :email WHERE uid = :userId")
    suspend fun updateUserProfile(userId: String, displayName: String, email: String): Int

    // Método adicional para verificar si se actualizó correctamente
    // Consulta local de lectura; devuelve el recuento del propietario indicado. Los filtros y parámetros de la consulta definen su alcance.
    @Query("SELECT COUNT(*) FROM usuarios WHERE uid = :userId")
    suspend fun userExists(userId: String): Int

    // Actualización local de filas según el estado de sincronización. Los filtros y parámetros de la consulta definen su alcance.
    @Query("UPDATE usuarios SET sincronizadoFirebase = 0 WHERE uid = :uid")
    suspend fun markUserNotSynced(uid: String)
}
