package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import es.nuskysoftware.marketsales.data.local.entity.UserEntity

@Dao
interface UserDao {

    // ✅ MÉTODOS BÁSICOS (igual que ConfiguracionDao)

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

    // ✅ MÉTODOS ESPECÍFICOS DE ACTUALIZACIÓN (patrón igual que ConfiguracionDao)

    @Query("UPDATE usuarios SET planUsuario = :planUsuario, empresaId = :empresaId WHERE uid = :uid")
    suspend fun updateUserPlan(uid: String, planUsuario: String, empresaId: String?)

    @Query("UPDATE usuarios SET tipoUsuario = :tipoUsuario WHERE uid = :uid")
    suspend fun updateUserType(uid: String, tipoUsuario: String?)

    @Query("UPDATE usuarios SET permisos = :permisos WHERE uid = :uid")
    suspend fun updateUserPermissions(uid: String, permisos: String?)

    @Query("UPDATE usuarios SET email = :email WHERE uid = :uid")
    suspend fun updateUserEmail(uid: String, email: String)

    @Query("UPDATE usuarios SET displayName = :displayName WHERE uid = :uid")
    suspend fun updateUserDisplayName(uid: String, displayName: String)

    @Query("UPDATE usuarios SET photoUrl = :photoUrl WHERE uid = :uid")
    suspend fun updateUserPhotoUrl(uid: String, photoUrl: String)

    @Query("UPDATE usuarios SET activo = :activo WHERE uid = :uid")
    suspend fun updateUserActive(uid: String, activo: Boolean)

    @Query("UPDATE usuarios SET fechaUltimaSync = :fecha WHERE uid = :uid")
    suspend fun updateFechaUltimaSync(uid: String, fecha: String?)

    // ✅ MÉTODOS OFFLINE-FIRST (igual que ConfiguracionDao)

    /** Marca si hay pendiente de sincronizar en Firebase */
    @Query("UPDATE usuarios SET pendienteSync = :pendiente WHERE uid = :uid")
    suspend fun updatePendienteSync(uid: String, pendiente: Boolean)

    /** Flujo que emite true/false según si hay pendienteSync */
    @Query("SELECT pendienteSync FROM usuarios WHERE uid = :uid")
    fun getPendienteSyncFlow(uid: String): Flow<Boolean>

    /** Todos los usuarios con sync pendiente */
    @Query("SELECT * FROM usuarios WHERE pendienteSync = 1")
    suspend fun getUsersPendingSyncSync(): List<UserEntity>

    // ✅ MÉTODOS PARA PREMIUM (empresas y colaboración)

    /** Usuarios de una empresa específica */
    @Query("SELECT * FROM usuarios WHERE empresaId = :empresaId AND activo = 1")
    suspend fun getUsersByCompany(empresaId: String): List<UserEntity>

    @Query("SELECT * FROM usuarios WHERE empresaId = :empresaId AND activo = 1")
    fun getUsersByCompanyFlow(empresaId: String): Flow<List<UserEntity>>

    /** Super Admins de una empresa */
    @Query("SELECT * FROM usuarios WHERE empresaId = :empresaId AND tipoUsuario = 'SUPER_ADMIN' AND activo = 1")
    suspend fun getSuperAdminsByCompany(empresaId: String): List<UserEntity>

    /** Contar usuarios de una empresa */
    @Query("SELECT COUNT(*) FROM usuarios WHERE empresaId = :empresaId AND activo = 1")
    suspend fun countUsersByCompany(empresaId: String): Int

    // ✅ MÉTODOS PARA BÚSQUEDAS COMUNES

    @Query("SELECT * FROM usuarios WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM usuarios WHERE planUsuario = 'PREMIUM'")
    suspend fun getAllPremiumUsers(): List<UserEntity>

    @Query("SELECT * FROM usuarios WHERE planUsuario = 'FREE'")
    suspend fun getAllFreeUsers(): List<UserEntity>

    @Query("SELECT * FROM usuarios WHERE activo = 1")
    suspend fun getAllActiveUsers(): List<UserEntity>

    // ✅ MÉTODOS DE LIMPIEZA

    @Query("DELETE FROM usuarios WHERE activo = 0")
    suspend fun deleteInactiveUsers()

    @Query("DELETE FROM usuarios")
    suspend fun deleteAllUsers()

    // ✅ MÉTODOS REACTIVOS PRINCIPALES (para UserRepository StateFlows)

    /** Usuario actual - para UserRepository.currentUser */
    @Query("SELECT * FROM usuarios WHERE uid = (SELECT usuarioId FROM configuracion WHERE id = 1) LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    /** Usuarios de la empresa del usuario actual - para UserRepository.companyUsers */
    @Query("""
        SELECT u.* FROM usuarios u 
        INNER JOIN configuracion c ON u.empresaId = (
            SELECT empresaId FROM usuarios WHERE uid = c.usuarioId
        ) 
        WHERE u.activo = 1 AND c.id = 1
    """)
    fun getCompanyUsersFlow(): Flow<List<UserEntity>>
}