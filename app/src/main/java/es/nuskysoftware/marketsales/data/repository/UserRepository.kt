// app/src/main/java/es/nuskysoftware/marketsales/data/repository/UserRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.data.local.dao.UserDao
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.UserEntity
import es.nuskysoftware.marketsales.utils.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await // ✅ IMPORT CORREGIDO
import java.text.SimpleDateFormat
import java.util.*

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

    // Exponer usuarios de empresa como StateFlow (para Premium)
    val companyUsers = userDao.getCompanyUsersFlow()
        .stateIn(repositoryScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Cuando volvemos online, sincronizamos usuarios pendientes
        repositoryScope.launch {
            connectivityObserver.isConnected.collect { online ->
                if (online) {
                    val pendingUsers = userDao.getUsersPendingSyncSync()
                    pendingUsers.forEach { user ->
                        sincronizarUsuarioConFirebase(user)
                    }
                }
            }
        }
    }

    // ✅ MÉTODO PRINCIPAL: Cargar datos completos del usuario desde Firebase
    suspend fun loadUserData(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            // 1. Intentar desde Room primero (offline-first)
            val localUser = userDao.getUserByIdSync(userId)

            // 2. Si existe localmente y no está pendiente de sincronización, devolverlo
            if (localUser != null && !localUser.pendienteSync) {
                return@withContext localUser
            }

            // 3. Intentar cargar desde Firebase si hay conexión
            if (connectivityObserver.isConnected.first()) {
                val document = firestore.collection("usuarios")
                    .document(userId)
                    .get()
                    .await()

                if (document.exists()) {
                    val firebaseUser = mapFirebaseToUserEntity(userId, document.data!!)

                    // Guardar en Room
                    userDao.insertOrUpdate(firebaseUser)

                    return@withContext firebaseUser
                }
            }

            // 4. Si no existe en Firebase, crear usuario con datos por defecto
            val defaultUser = createDefaultUser(userId)
            userDao.insertOrUpdate(defaultUser)
            sincronizarUsuarioConFirebase(defaultUser)

            return@withContext defaultUser

        } catch (e: Exception) {
            // En caso de error, devolver datos locales si existen
            return@withContext userDao.getUserByIdSync(userId)
        }
    }

    // ✅ ACTUALIZAR PLAN DE USUARIO (FREE → PREMIUM)
    suspend fun updateUserPlan(userId: String, planUsuario: String, empresaId: String? = null) = withContext(Dispatchers.IO) {
        userDao.updateUserPlan(userId, planUsuario, empresaId)
        userDao.updatePendienteSync(userId, true)
        sincronizarCampoEspecifico(userId, "planUsuario", planUsuario)

        if (empresaId != null) {
            sincronizarCampoEspecifico(userId, "empresaId", empresaId)
        }
    }

    // ✅ CREAR EMPRESA (para usuarios que compran Premium)
    suspend fun createCompany(userId: String, companyName: String, companyEmail: String): String = withContext(Dispatchers.IO) {
        val empresaId = "empresa_${System.currentTimeMillis()}"

        // Crear empresa en Firebase
        val empresaData = mapOf(
            "id" to empresaId,
            "nombre" to companyName,
            "email" to companyEmail,
            "superAdminId" to userId,
            "fechaCreacion" to dateFormat.format(Date()),
            "activa" to true
        )

        try {
            firestore.collection("empresas")
                .document(empresaId)
                .set(empresaData)
                .await()

            // Actualizar usuario a SUPER_ADMIN de esta empresa
            updateUserPlan(userId, "PREMIUM", empresaId)
            updateUserType(userId, "SUPER_ADMIN")

            return@withContext empresaId

        } catch (e: Exception) {
            // En caso de error, marcar como pendiente
            userDao.updatePendienteSync(userId, true)
            throw e
        }
    }

    // ✅ ACTUALIZAR TIPO DE USUARIO
    suspend fun updateUserType(userId: String, tipoUsuario: String) = withContext(Dispatchers.IO) {
        userDao.updateUserType(userId, tipoUsuario)
        userDao.updatePendienteSync(userId, true)
        sincronizarCampoEspecifico(userId, "tipoUsuario", tipoUsuario)
    }

    // ✅ SINCRONIZACIÓN COMPLETA DEL USUARIO
    suspend fun sincronizar(userId: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserByIdSync(userId) ?: return@withContext
        sincronizarUsuarioConFirebase(user)
    }

    // 🔄 MÉTODOS PRIVADOS DE SINCRONIZACIÓN

    /**
     * Sincroniza usuario completo con Firebase
     */
    private fun sincronizarUsuarioConFirebase(user: UserEntity) {
        try {
            val datos = mapOf(
                "email" to user.email,
                "uid" to user.uid,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "planUsuario" to user.planUsuario,
                "empresaId" to user.empresaId,
                "tipoUsuario" to user.tipoUsuario,
                // ✅ CORRECCIÓN: Permisos como string simple por ahora
                "permisos" to user.permisos,
                "fechaCreacion" to user.fechaCreacion,
                "fechaUltimaSync" to dateFormat.format(Date()),
                "activo" to user.activo
            )

            val docRef = firestore.collection("usuarios")
                .document(user.uid)

            docRef.set(datos)
                .addOnSuccessListener {
                    repositoryScope.launch {
                        userDao.updateFechaUltimaSync(user.uid, dateFormat.format(Date()))
                        userDao.updatePendienteSync(user.uid, false)
                    }
                }
                .addOnFailureListener {
                    repositoryScope.launch {
                        userDao.updatePendienteSync(user.uid, true)
                    }
                }

        } catch (e: Exception) {
            repositoryScope.launch {
                userDao.updatePendienteSync(user.uid, true)
            }
        }
    }

    /**
     * Sincroniza un campo específico del usuario
     */
    private suspend fun sincronizarCampoEspecifico(userId: String, campo: String, valor: Any) {
        try {
            val datos = mapOf(campo to valor, "fechaUltimaSync" to dateFormat.format(Date()))

            firestore.collection("usuarios")
                .document(userId)
                .update(datos)
                .await()

            userDao.updateFechaUltimaSync(userId, dateFormat.format(Date()))
            userDao.updatePendienteSync(userId, false)

        } catch (e: Exception) {
            userDao.updatePendienteSync(userId, true)
        }
    }

    // 🏗️ MÉTODOS AUXILIARES

    /**
     * Convierte datos de Firebase a UserEntity
     */
    private fun mapFirebaseToUserEntity(userId: String, data: Map<String, Any>): UserEntity {
        return UserEntity(
            uid = userId,
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String ?: "",
            photoUrl = data["photoUrl"] as? String ?: "",
            planUsuario = data["planUsuario"] as? String ?: "FREE",
            empresaId = data["empresaId"] as? String,
            tipoUsuario = data["tipoUsuario"] as? String,
            permisos = data["permisos"] as? String, // ✅ CORRECCIÓN: String simple
            fechaCreacion = data["fechaCreacion"] as? Long ?: System.currentTimeMillis(),
            fechaUltimaSync = dateFormat.format(Date()),
            pendienteSync = false,
            activo = data["activo"] as? Boolean ?: true
        )
    }

    // ✅ INVITAR USUARIO A EMPRESA (AGREGAR ESTE MÉTODO)
    suspend fun inviteUserToCompany(email: String, empresaId: String, tipoUsuario: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Crear invitación en Firebase
            val invitacionData = mapOf(
                "email" to email,
                "empresaId" to empresaId,
                "tipoUsuario" to tipoUsuario,
                "fechaInvitacion" to dateFormat.format(Date()),
                "estado" to "PENDIENTE"
            )

            firestore.collection("invitaciones")
                .add(invitacionData)
                .await()

            return@withContext true

        } catch (e: Exception) {
            return@withContext false
        }
    }
    /**
     * Crea usuario con configuración por defecto
     */
    private fun createDefaultUser(userId: String): UserEntity {
        return UserEntity(
            uid = userId,
            email = "",
            displayName = "",
            photoUrl = "",
            planUsuario = "FREE",
            empresaId = null,
            tipoUsuario = null,
            permisos = null,
            fechaCreacion = System.currentTimeMillis(),
            fechaUltimaSync = null,
            pendienteSync = true,
            activo = true
        )
    }
}