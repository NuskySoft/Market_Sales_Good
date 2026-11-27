// app/src/main/java/es/nuskysoftware/marketsales/data/repository/UserRepository.kt
package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Gestiona la información de perfil y las operaciones de persistencia del usuario autenticado.
import es.nuskysoftware.marketsales.utils.DemoAccess


import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.UserDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.UserEntity
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * UserRepository V10 - SIMPLIFICADO PARA SISTEMA MONOUSUARIO
 *
 * CAMBIOS V10:
 * - Eliminado sistema multiusuario (empresas, invitaciones, roles)
 * - Eliminado configuración personal (ahora es global)
 * - Solo gestiona: datos básicos + esPremium + sincronización
 * - Agregado getOrCreateUser() para AuthRepository
 */
class UserRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val userDao: UserDao = AppDatabase.getDatabase(context).userDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Observador de red
    private val connectivityObserver = ConnectivityObserver(context)

    // Exponer usuario actual como StateFlow
    val currentUser = ConfigurationManager.usuarioLogueado.flatMapLatest { uid -> if (uid == null) flowOf(null) else userDao.getUserByIdFlow(uid) }
        .stateIn(repositoryScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    // ========== MÉTODOS PRINCIPALES V10 ==========

    /**
     * ✅ NUEVO: Busca usuario existente o crea uno nuevo (para AuthRepository)
     */
    suspend fun getOrCreateUser(
        uid: String,
        email: String,
        displayName: String = "",
        photoUrl: String = "",
        esPremium: Boolean = false
    ): UserEntity = withContext(Dispatchers.IO) {
        try {
            // 1. Buscar usuario existente en Room
            val existingUser = userDao.getUserByIdSync(uid)
            if (existingUser != null) {
                // Actualizar datos básicos si han cambiado
                if (existingUser.email != email ||
                    existingUser.displayName != displayName ||
                    existingUser.photoUrl != photoUrl
                ) {

                    val updatedUser = existingUser.copy(
                        email = email,
                        displayName = displayName,
                        photoUrl = photoUrl
                    )
                    userDao.insertOrUpdate(updatedUser)
                    sincronizarUsuarioConFirebase(updatedUser)
                    return@withContext updatedUser
                }
                return@withContext existingUser
            }

            // 2. Si no existe, buscar en Firebase
            if (connectivityObserver.isConnected.first()) {
                try {
                    val document = firestore.collection("usuarios")
                        .document(uid)
                        .get()
                        .await()

                    if (document.exists()) {
                        val firebaseUser = mapFirebaseToUserEntity(uid, document.data!!)
                        userDao.insertOrUpdate(firebaseUser)
                        return@withContext firebaseUser
                    }
                } catch (e: Exception) {
                    // Si falla Firebase, crear usuario local
                }
            }

            // 3. Crear usuario nuevo
            val newUser = UserEntity(
                uid = uid,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                esPremium = esPremium
            )

            userDao.insertOrUpdate(newUser)
            sincronizarUsuarioConFirebase(newUser)

            return@withContext newUser

        } catch (e: Exception) {
            // En caso de error, crear usuario mínimo
            val fallbackUser = UserEntity(
                uid = uid,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                esPremium = esPremium
            )
            userDao.insertOrUpdate(fallbackUser)
            return@withContext fallbackUser
        }
    }

    /**
     * Obtiene un usuario por ID
     */
    suspend fun getUserById(uid: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByIdSync(uid)
    }

    /**
     * Actualiza el estado Premium del usuario
     */
    suspend fun updateUserPremium(uid: String, esPremium: Boolean) = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(uid)
        check(DemoAccess.canSync(uid))
        firestore.collection("usuarios").document(uid).update("esPremium", esPremium).await()
        DemoAccess.requireOwner(uid)
        userDao.updateUserPremium(uid, esPremium)
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    /**
     * Actualiza el email del usuario
     */
    suspend fun updateUserEmail(uid: String, email: String) = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(uid)
        val old = userDao.getUserByIdSync(uid) ?: error("Account not found")
        userDao.insertOrUpdate(old.copy(email = email, version = old.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    /**
     * Actualiza el nombre del usuario
     */
    suspend fun updateUserDisplayName(uid: String, displayName: String) =
        withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(uid)
        val old = userDao.getUserByIdSync(uid) ?: error("Account not found")
        userDao.insertOrUpdate(old.copy(displayName = displayName, version = old.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    /**
     * Actualiza la foto del usuario
     */
    suspend fun updateUserPhotoUrl(uid: String, photoUrl: String) = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(uid)
        val old = userDao.getUserByIdSync(uid) ?: error("Account not found")
        userDao.insertOrUpdate(old.copy(photoUrl = photoUrl, version = old.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    /**
     * Busca usuario por email
     */
    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }

    /**
     * Obtiene todos los usuarios Premium
     */
    suspend fun getAllPremiumUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getAllPremiumUsers()
    }

    /**
     * Obtiene todos los usuarios Free
     */
    suspend fun getAllFreeUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getAllFreeUsers()
    }

    /**
     * Sincroniza un usuario específico
     */
    suspend fun sincronizar(userId: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserByIdSync(userId) ?: return@withContext
        sincronizarUsuarioConFirebase(user)
    }

    // ========== MÉTODOS DE SINCRONIZACIÓN V10 ==========

    /**
     * Sincroniza usuario completo con Firebase
     */
    private fun sincronizarUsuarioConFirebase(user: UserEntity) {
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    /**
     * Sincroniza un campo específico del usuario
     */
    private suspend fun sincronizarCampoEspecifico(userId: String, campo: String, valor: Any) {
        if (DemoAccess.canSync(userId)) es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }

    // ========== MÉTODOS AUXILIARES ==========

    /**
     * Convierte datos de Firebase a UserEntity V10
     */
    private fun mapFirebaseToUserEntity(userId: String, data: Map<String, Any>): UserEntity {
        return UserEntity(
            uid = userId,
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String ?: "",
            photoUrl = data["photoUrl"] as? String ?: "",
            esPremium = data["esPremium"] as? Boolean ?: false,
            version = data["version"] as? Long ?: 1L,
            lastModified = data["lastModified"] as? Long ?: System.currentTimeMillis(),
            sincronizadoFirebase = true, // Viene de Firebase, está sincronizado
            fechaCreacion = data["fechaCreacion"] as? Long ?: System.currentTimeMillis(),
            fechaUltimaSync = dateFormat.format(Date()),
            activo = data["activo"] as? Boolean ?: true
        )
    }

    // ========== MÉTODOS OBSOLETOS V10 (compatibilidad) ==========

    @Deprecated(
        "Usar getOrCreateUser()",
        ReplaceWith("getOrCreateUser(userId, \"\", \"\", \"\", false)")
    )
    suspend fun loadUserData(userId: String): UserEntity? {
        return getOrCreateUser(userId, "", "", "", false)
    }

    @Deprecated(
        "Sistema multiusuario eliminado en V10",
        ReplaceWith("updateUserPremium(userId, premium)")
    )
    suspend fun updateUserPlan(userId: String, planUsuario: String, empresaId: String? = null) {
        updateUserPremium(userId, planUsuario == "PREMIUM")
    }

    @Deprecated(
        "Sistema multiusuario eliminado en V10",
        ReplaceWith("updateUserPremium(userId, true)")
    )
    suspend fun createCompany(userId: String, companyName: String, companyEmail: String): String {
        updateUserPremium(userId, true)
        return "empresa_${System.currentTimeMillis()}" // ID dummy para compatibilidad
    }

    @Deprecated(
        "Sistema multiusuario eliminado en V10",
        ReplaceWith("updateUserPremium(userId, true)")
    )
    suspend fun updateUserType(userId: String, tipoUsuario: String) {
        // En V10, solo distinguimos Premium/Free
        updateUserPremium(userId, tipoUsuario != "FREE")
    }

    @Deprecated("Sistema multiusuario eliminado en V10", ReplaceWith("false"))
    suspend fun inviteUserToCompany(
        email: String,
        empresaId: String,
        tipoUsuario: String
    ): Boolean {
        return false // No soportado en V10
    }

    @Deprecated(
        "Configuración personal eliminada en V10 - ahora global",
        ReplaceWith("ConfiguracionRepository.updateIdioma()")
    )
    suspend fun updateUserIdioma(userId: String, idioma: String) {
        // No-op en V10
    }

    @Deprecated(
        "Configuración personal eliminada en V10 - ahora global",
        ReplaceWith("ConfiguracionRepository.updateFuente()")
    )
    suspend fun updateUserFuente(userId: String, fuente: String) {
        // No-op en V10
    }

    @Deprecated(
        "Configuración personal eliminada en V10 - ahora global",
        ReplaceWith("ConfiguracionRepository.updateTemaOscuro()")
    )
    suspend fun updateUserModoOscuro(userId: String, modoOscuro: Boolean) {
        // No-op en V10
    }

    @Deprecated(
        "Configuración personal eliminada en V10 - ahora global",
        ReplaceWith("ConfiguracionRepository métodos individuales")
    )
    suspend fun updateUserConfiguration(
        userId: String,
        idioma: String,
        fuente: String,
        modoOscuro: Boolean
    ) {
        // No-op en V10 - La configuración ahora es global
    }
    // ========== REEMPLAZA el método getHybridUserData() en UserRepository.kt ==========

    suspend fun getHybridUserData(uid: String): UserEntity? = withContext(Dispatchers.IO) {
        DemoAccess.requireOwner(uid)
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
        userDao.getUserByIdSync(uid)
    }
    // En UserRepository.kt
    suspend fun updateUserProfile(userId: String, displayName: String, email: String) {
        DemoAccess.requireOwner(userId)
        val old = userDao.getUserByIdSync(userId) ?: error("Account not found")
        userDao.insertOrUpdate(old.copy(displayName = displayName, email = email, version = old.version + 1, lastModified = System.currentTimeMillis(), sincronizadoFirebase = false))
        es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
    }
    suspend fun updateUserProfileAndMarkDirty(userId: String, displayName: String, email: String) = withContext(Dispatchers.IO) {
        updateUserProfile(userId, displayName, email)
    }
    suspend fun refreshUserData(uid: String) = withContext(Dispatchers.IO) {
        try {
            // Forzar lectura de Firebase y actualizar Room
            if (connectivityObserver.isConnected.first()) {
                val firebaseDoc = firestore.collection("usuarios")
                    .document(uid)
                    .get()
                    .await()

                if (firebaseDoc.exists()) {
                    val firebaseUser = mapFirebaseToUserEntity(uid, firebaseDoc.data!!)
                    userDao.insertOrUpdate(firebaseUser.copy(sincronizadoFirebase = true))
                    Log.d("UserRepository", "✅ Datos de usuario refrescados desde Firebase")
                }
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "❌ Error refrescando datos de usuario", e)
        }
    }

    // Método auxiliar para obtener usuario actual (si no existe)
    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        currentUser.value
    }
}
