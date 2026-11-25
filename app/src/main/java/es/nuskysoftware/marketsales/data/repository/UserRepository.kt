// app/src/main/java/es/nuskysoftware/marketsales/data/repository/UserRepository.kt
package es.nuskysoftware.marketsales.data.repository
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
    private val userDao: UserDao = AppDatabase.getDatabase(context).userDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Observador de red
    private val connectivityObserver = ConnectivityObserver(context)

    // Exponer usuario actual como StateFlow
    val currentUser = userDao.getCurrentUserFlow()
        .stateIn(repositoryScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Cuando volvemos online, sincronizamos usuarios pendientes
        repositoryScope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    val pendingUsers = userDao.getUsersPendingSync()
                    pendingUsers.forEach { user ->
                        sincronizarUsuarioConFirebase(user)
                    }
                }
            }
        }
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
        userDao.updateUserPremium(uid, esPremium)
        sincronizarCampoEspecifico(uid, "esPremium", esPremium)
    }

    /**
     * Actualiza el email del usuario
     */
    suspend fun updateUserEmail(uid: String, email: String) = withContext(Dispatchers.IO) {
        userDao.updateUserEmail(uid, email)
        sincronizarCampoEspecifico(uid, "email", email)
    }

    /**
     * Actualiza el nombre del usuario
     */
    suspend fun updateUserDisplayName(uid: String, displayName: String) =
        withContext(Dispatchers.IO) {
            userDao.updateUserDisplayName(uid, displayName)
            sincronizarCampoEspecifico(uid, "displayName", displayName)
        }

    /**
     * Actualiza la foto del usuario
     */
    suspend fun updateUserPhotoUrl(uid: String, photoUrl: String) = withContext(Dispatchers.IO) {
        userDao.updateUserPhotoUrl(uid, photoUrl)
        sincronizarCampoEspecifico(uid, "photoUrl", photoUrl)
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
        if (!DemoAccess.canSync(user.uid)) return
        try {
            val datos = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "esPremium" to user.esPremium,
                "version" to user.version,
                "lastModified" to user.lastModified,
                "fechaCreacion" to user.fechaCreacion,
                "fechaUltimaSync" to dateFormat.format(Date()),
                "activo" to user.activo
            )

            val docRef = firestore.collection("usuarios")
                .document(user.uid)

            docRef.set(datos)
                .addOnSuccessListener {
                    repositoryScope.launch {
                        userDao.markUserSyncSuccessful(user.uid, user.version + 1)
                        userDao.updateFechaUltimaSync(user.uid, dateFormat.format(Date()))
                    }
                }
                .addOnFailureListener {
                    repositoryScope.launch {
                        // El usuario queda marcado como no sincronizado hasta próximo intento
                    }
                }

        } catch (e: Exception) {
            // Error en sincronización, se reintentará cuando haya conexión
        }
    }

    /**
     * Sincroniza un campo específico del usuario
     */
    private suspend fun sincronizarCampoEspecifico(userId: String, campo: String, valor: Any) {
        try {
            val datos = mapOf(
                campo to valor,
                "lastModified" to System.currentTimeMillis(),
                "fechaUltimaSync" to dateFormat.format(Date())
            )

            firestore.collection("usuarios")
                .document(userId)
                .update(datos)
                .await()

            // Marcar como sincronizado
            userDao.markUserSyncSuccessful(userId, userDao.getUserVersion(userId) ?: 1L + 1)

        } catch (e: Exception) {
            // El campo queda marcado como no sincronizado
        }
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
        try {
            // 1. Verificar si hay datos pendientes de sincronizar en Room
            val roomUser = userDao.getUserByIdSync(uid)
            val hasPendingChanges = roomUser?.sincronizadoFirebase == false

            if (hasPendingChanges) {
                // ✅ HAY CAMBIOS PENDIENTES → Leer de Room (fuente de verdad)
                Log.d("UserRepository", "📱 Leyendo de ROOM (cambios pendientes): $uid")
                return@withContext roomUser
            } else {
                // ✅ NO HAY CAMBIOS PENDIENTES → Leer de Firebase (más actualizado)
                Log.d("UserRepository", "☁️ Leyendo de FIREBASE (sin cambios pendientes): $uid")

                if (connectivityObserver.isConnected.first()) {
                    try {
                        val firebaseDoc = firestore.collection("usuarios")
                            .document(uid)
                            .get()
                            .await()

                        if (firebaseDoc.exists()) {
                            val firebaseUser = mapFirebaseToUserEntity(uid, firebaseDoc.data!!)

                            // Actualizar Room con datos frescos de Firebase
                            userDao.insertOrUpdate(firebaseUser.copy(sincronizadoFirebase = true))

                            Log.d("UserRepository", "✅ Datos frescos de Firebase aplicados a Room")
                            return@withContext firebaseUser
                        }
                    } catch (e: Exception) {
                        Log.w(
                            "UserRepository",
                            "❌ Error leyendo Firebase, usando Room como fallback",
                            e
                        )
                    }
                }

                // Fallback: usar Room si Firebase falla o no hay conexión
                return@withContext roomUser ?: getOrCreateUser(uid, "", "", "", false)
            }

        } catch (e: Exception) {
            Log.e("UserRepository", "Error en estrategia híbrida", e)
            // En caso de error, devolver lo que tengamos en Room o crear usuario básico
            val fallbackUser = userDao.getUserByIdSync(uid)
            return@withContext fallbackUser ?: getOrCreateUser(uid, "", "", "", false)
        }
    }
    // En UserRepository.kt
    suspend fun updateUserProfile(userId: String, displayName: String, email: String) {
        try {
            userDao.updateUserProfile(userId, displayName, email)
            Log.d(TAG, "✅ Perfil actualizado en Room: $displayName, $email")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando perfil en Room", e)
        }
    }
    suspend fun updateUserProfileAndMarkDirty(userId: String, displayName: String, email: String) = withContext(Dispatchers.IO) {
        try {
            // 1. Actualizar los datos en Room
            userDao.updateUserProfile(userId, displayName, email)

            // 2. ✅ CLAVE: Marcar como NO SINCRONIZADO para que getHybridUserData use Room
            userDao.markUserNotSynced(userId)

            Log.d("UserRepository", "✅ Perfil actualizado y marcado como no sincronizado")
            Log.d("UserRepository", "   - displayName: $displayName")
            Log.d("UserRepository", "   - sincronizadoFirebase: false")

        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Error actualizando perfil", e)
        }
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