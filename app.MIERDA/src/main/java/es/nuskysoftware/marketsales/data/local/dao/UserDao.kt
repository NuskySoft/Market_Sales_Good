// app/src/main/java/es/nuskysoftware/marketsales/data/local/dao/UserDao.kt
package es.nuskysoftware.marketsales.data.local.dao

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
    @Query("SELECT EXISTS(SELECT 1 FROM usuarios WHERE uid = :uid LIMIT 1)")
    suspend fun existeUsuario(uid: String): Boolean

    // ¿Es Premium el usuario? (null si no existe)
    @Query("SELECT esPremium FROM usuarios WHERE uid = :uid")
    suspend fun isPremium(uid: String): Boolean?

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    suspend fun getUserByIdSync(uid: String): UserEntity?

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
    @Query("SELECT * FROM usuarios WHERE uid != 'dummy'")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    /**
     * Obtener usuarios pendientes de sincronización
     */
    @Query("SELECT * FROM usuarios WHERE sincronizadoFirebase = 0")
    suspend fun getUsersPendingSync(): List<UserEntity>

    /**
     * Actualizar estado Premium del usuario
     */
    @Query("UPDATE usuarios SET esPremium = :esPremium WHERE uid = :uid")
    suspend fun updateUserPremium(uid: String, esPremium: Boolean)

    /**
     * Actualizar email del usuario
     */
    @Query("UPDATE usuarios SET email = :email WHERE uid = :uid")
    suspend fun updateUserEmail(uid: String, email: String)

    /**
     * Actualizar nombre del usuario
     */
    @Query("UPDATE usuarios SET displayName = :displayName WHERE uid = :uid")
    suspend fun updateUserDisplayName(uid: String, displayName: String)

    /**
     * Actualizar foto del usuario
     */
    @Query("UPDATE usuarios SET photoUrl = :photoUrl WHERE uid = :uid")
    suspend fun updateUserPhotoUrl(uid: String, photoUrl: String)

    /**
     * Obtener todos los usuarios Premium
     */
    @Query("SELECT * FROM usuarios WHERE esPremium = 1 AND activo = 1")
    suspend fun getAllPremiumUsers(): List<UserEntity>

    /**
     * Obtener todos los usuarios Free
     */
    @Query("SELECT * FROM usuarios WHERE esPremium = 0 AND activo = 1")
    suspend fun getAllFreeUsers(): List<UserEntity>

    /**
     * Marcar usuario como sincronizado exitosamente
     */
    @Query("UPDATE usuarios SET sincronizadoFirebase = 1, version = :newVersion WHERE uid = :uid")
    suspend fun markUserSyncSuccessful(uid: String, newVersion: Long)

    /**
     * Actualizar fecha de última sincronización
     */
    @Query("UPDATE usuarios SET fechaUltimaSync = :fecha WHERE uid = :uid")
    suspend fun updateFechaUltimaSync(uid: String, fecha: String)

    /**
     * Obtener versión de usuario
     */
    @Query("SELECT version FROM usuarios WHERE uid = :uid")
    suspend fun getUserVersion(uid: String): Long?

    // ========== MÉTODOS EXISTENTES ==========
    @Query("SELECT * FROM usuarios WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM usuarios WHERE activo = 1")
    suspend fun getAllActiveUsers(): List<UserEntity>

    // En UserDao.kt - Alternativa más robusta:

    @Query("UPDATE usuarios SET displayName = :displayName, email = :email WHERE uid = :userId")
    suspend fun updateUserProfile(userId: String, displayName: String, email: String): Int

    // Método adicional para verificar si se actualizó correctamente
    @Query("SELECT COUNT(*) FROM usuarios WHERE uid = :userId")
    suspend fun userExists(userId: String): Int

    @Query("UPDATE usuarios SET sincronizadoFirebase = 0 WHERE uid = :uid")
    suspend fun markUserNotSynced(uid: String)
}

