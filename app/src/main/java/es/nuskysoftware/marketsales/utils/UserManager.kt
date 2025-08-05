// app/src/main/java/es/nuskysoftware/marketsales/utils/UserManager.kt
package es.nuskysoftware.marketsales.utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import es.nuskysoftware.marketsales.data.repository.UserRepository
import es.nuskysoftware.marketsales.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UserManager - Singleton para gestionar usuarios y permisos
 * ✅ FASE 1D: Integración completa con UserRepository + ConfigurationManager + AuthRepository
 */
object UserManager {

    private const val TAG = "UserManager"
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Estados internos
    private val _currentUserEntity = MutableStateFlow<UserEntity?>(null)
    private val _isInitialized = MutableStateFlow(false)

    // Estados públicos
    val currentUserEntity: StateFlow<UserEntity?> = _currentUserEntity.asStateFlow()
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Referencias a repositorios
    private var userRepository: UserRepository? = null
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // ✅ INICIALIZACIÓN DEL SISTEMA
    fun initialize(context: Context) {
        if (_isInitialized.value) {
            Log.d(TAG, "UserManager ya inicializado")
            return
        }

        try {
            Log.d(TAG, "Inicializando UserManager...")

            // Inicializar UserRepository
            userRepository = UserRepository(context)

            // Escuchar cambios de Firebase Auth
            firebaseAuth.addAuthStateListener { auth ->
                val firebaseUser = auth.currentUser
                managerScope.launch {
                    handleAuthStateChange(firebaseUser)
                }
            }

            // Cargar usuario actual si existe
            val currentFirebaseUser = firebaseAuth.currentUser
            managerScope.launch {
                handleAuthStateChange(currentFirebaseUser)
                _isInitialized.value = true
            }

            Log.d(TAG, "✅ UserManager inicializado correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando UserManager", e)
            _isInitialized.value = true // Marcar como inicializado para evitar bucles
        }
    }

    // ✅ MANEJAR CAMBIOS DE ESTADO DE AUTENTICACIÓN
    private suspend fun handleAuthStateChange(firebaseUser: FirebaseUser?) {
        try {
            if (firebaseUser != null) {
                Log.d(TAG, "Usuario autenticado: ${firebaseUser.email}")

                // Cargar datos del usuario desde UserRepository
                val userEntity = userRepository?.loadUserData(firebaseUser.uid)

                if (userEntity != null) {
                    // Actualizar estado local
                    _currentUserEntity.value = userEntity

                    // Sincronizar con ConfigurationManager
                    ConfigurationManager.authenticateUser(
                        email = userEntity.email,
                        userId = userEntity.uid,
                        displayName = userEntity.displayName,
                        photoUrl = userEntity.photoUrl,
                        planUsuario = userEntity.planUsuario,
                        empresaId = userEntity.empresaId,
                        tipoUsuario = userEntity.tipoUsuario
                    )

                    Log.d(TAG, "✅ Usuario cargado: ${userEntity.planUsuario}, empresa: ${userEntity.empresaId}")
                } else {
                    Log.e(TAG, "No se pudo cargar datos del usuario")
                }

            } else {
                Log.d(TAG, "Usuario no autenticado")

                // Limpiar estado local
                _currentUserEntity.value = null

                // Cargar configuración de usuario_default
                val defaultUser = userRepository?.loadUserData("usuario_default")

                // Sincronizar con ConfigurationManager
                ConfigurationManager.logoutUser()

                Log.d(TAG, "✅ Usuario desautenticado, configuración default cargada")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error manejando cambio de estado de autenticación", e)
        }
    }

    // ✅ MÉTODOS DE VERIFICACIÓN DE PERMISOS (delegados a ConfigurationManager)
    fun getCurrentScope(): ConfigurationManager.DataScope {
        return ConfigurationManager.getCurrentScope()
    }

    fun isPremium(): Boolean = ConfigurationManager.isPremium()
    fun isFree(): Boolean = ConfigurationManager.isFree()
    fun isSuperAdmin(): Boolean = ConfigurationManager.isSuperAdmin()
    fun isAdmin(): Boolean = ConfigurationManager.isAdmin()
    fun canManageUsers(): Boolean = ConfigurationManager.canManageUsers()
    fun canCreateContent(): Boolean = ConfigurationManager.canCreateContent()
    fun canDeleteContent(): Boolean = ConfigurationManager.canDeleteContent()

    // ✅ MÉTODOS DE GESTIÓN DE USUARIOS (delegados a UserRepository)
    suspend fun updateUserPlan(userId: String, planUsuario: String, empresaId: String? = null): Boolean {
        return try {
            userRepository?.updateUserPlan(userId, planUsuario, empresaId)

            // Recargar usuario actual si es el mismo que se está actualizando
            if (userId == _currentUserEntity.value?.uid) {
                val updatedUser = userRepository?.loadUserData(userId)
                _currentUserEntity.value = updatedUser

                updatedUser?.let { user ->
                    ConfigurationManager.authenticateUser(
                        email = user.email,
                        userId = user.uid,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl,
                        planUsuario = user.planUsuario,
                        empresaId = user.empresaId,
                        tipoUsuario = user.tipoUsuario
                    )
                }
            }

            Log.d(TAG, "✅ Plan de usuario actualizado: $planUsuario")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando plan de usuario", e)
            false
        }
    }

    suspend fun createCompany(userId: String, companyName: String, companyEmail: String): String? {
        return try {
            val empresaId = userRepository?.createCompany(userId, companyName, companyEmail)

            if (empresaId != null) {
                Log.d(TAG, "✅ Empresa creada: $empresaId")

                // Recargar usuario actual
                if (userId == _currentUserEntity.value?.uid) {
                    val updatedUser = userRepository?.loadUserData(userId)
                    _currentUserEntity.value = updatedUser

                    updatedUser?.let { user ->
                        ConfigurationManager.authenticateUser(
                            email = user.email,
                            userId = user.uid,
                            displayName = user.displayName,
                            photoUrl = user.photoUrl,
                            planUsuario = user.planUsuario,
                            empresaId = user.empresaId,
                            tipoUsuario = user.tipoUsuario
                        )
                    }
                }
            }

            empresaId

        } catch (e: Exception) {
            Log.e(TAG, "Error creando empresa", e)
            null
        }
    }

    suspend fun inviteUserToCompany(email: String, empresaId: String, tipoUsuario: String): Boolean {
        return try {
            val result = userRepository?.inviteUserToCompany(email, empresaId, tipoUsuario) ?: false

            if (result) {
                Log.d(TAG, "✅ Usuario invitado: $email a empresa $empresaId")
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Error invitando usuario", e)
            false
        }
    }

    // ✅ MÉTODOS DE CONSULTA
    fun getCurrentUser(): UserEntity? {
        return _currentUserEntity.value
    }

    fun getCurrentUserId(): String? {
        return _currentUserEntity.value?.uid
    }

    fun getCurrentUserEmail(): String? {
        return _currentUserEntity.value?.email
    }

    fun getCurrentCompanyId(): String? {
        return _currentUserEntity.value?.empresaId
    }

    fun isUserAuthenticated(): Boolean {
        return _currentUserEntity.value != null && ConfigurationManager.getIsAuthenticated()
    }

    // ✅ MÉTODOS PARA QUERIES DE DATOS
    fun getDataFilters(): Map<String, Any?> {
        val scope = getCurrentScope()

        return when (scope) {
            is ConfigurationManager.DataScope.Personal -> {
                mapOf(
                    "empresaId" to null,
                    "creadoPor" to scope.userId
                )
            }
            is ConfigurationManager.DataScope.Empresa -> {
                mapOf(
                    "empresaId" to scope.empresaId
                )
            }
        }
    }

    // ✅ MÉTODO PARA DEBUGGING
    fun printCurrentState() {
        Log.d(TAG, "🔍 UserManager Estado Actual:")
        val user = _currentUserEntity.value
        if (user != null) {
            Log.d(TAG, "   - Usuario: ${user.email}")
            Log.d(TAG, "   - Plan: ${user.planUsuario}")
            Log.d(TAG, "   - Empresa: ${user.empresaId}")
            Log.d(TAG, "   - Tipo: ${user.tipoUsuario}")
            Log.d(TAG, "   - Scope: ${getCurrentScope()}")
        } else {
            Log.d(TAG, "   - Sin usuario autenticado")
        }

        // También imprimir estado de ConfigurationManager
        ConfigurationManager.printCurrentState()
    }

    // ✅ MÉTODO PARA TESTING (útil para desarrollo)
    suspend fun testUserSystem(): Boolean {
        return try {
            Log.d(TAG, "🧪 Testing sistema de usuarios...")

            val firebaseUser = firebaseAuth.currentUser
            Log.d(TAG, "   - Firebase User: ${firebaseUser?.email ?: "null"}")

            val userEntity = _currentUserEntity.value
            Log.d(TAG, "   - UserEntity: ${userEntity?.email ?: "null"}")

            val configManager = ConfigurationManager.getCurrentConfiguration()
            Log.d(TAG, "   - ConfigurationManager isAuthenticated: ${configManager["isAuthenticated"]}")

            val scope = getCurrentScope()
            Log.d(TAG, "   - Current Scope: $scope")

            val filters = getDataFilters()
            Log.d(TAG, "   - Data Filters: $filters")

            Log.d(TAG, "✅ Test completado")
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en test", e)
            false
        }
    }

    // ✅ MÉTODO DE MIGRACIÓN FREE → PREMIUM (flujo completo)
    suspend fun upgradeToPremium(companyName: String, companyEmail: String): Boolean {
        return try {
            val currentUser = _currentUserEntity.value
            val firebaseUser = firebaseAuth.currentUser

            if (currentUser == null || firebaseUser == null) {
                Log.e(TAG, "No hay usuario autenticado para upgrade")
                return false
            }

            Log.d(TAG, "🚀 Iniciando upgrade a Premium para: ${currentUser.email}")

            // 1. Crear empresa
            val empresaId = createCompany(currentUser.uid, companyName, companyEmail)

            if (empresaId == null) {
                Log.e(TAG, "Error creando empresa para upgrade")
                return false
            }

            // 2. El createCompany() ya actualiza el usuario a PREMIUM + SUPER_ADMIN

            Log.d(TAG, "✅ Upgrade a Premium completado exitosamente")
            Log.d(TAG, "   - Empresa ID: $empresaId")
            Log.d(TAG, "   - Usuario es ahora: PREMIUM + SUPER_ADMIN")

            // 3. Testing final
            printCurrentState()

            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en upgrade a Premium", e)
            false
        }
    }

    // ✅ MÉTODO DE LIMPIEZA (para testing o reinicio)
    fun reset() {
        _currentUserEntity.value = null
        _isInitialized.value = false
        userRepository = null
        Log.d(TAG, "🔄 UserManager reseteado")
    }
}

//package es.nuskysoftware.marketsales.utils
//
//import android.util.Log
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
///**
// * UserManager - FASE 1A
// * Gestión centralizada de usuarios y permisos para FREE/PREMIUM
// */
//object UserManager {
//
//    private const val TAG = "UserManager"
//
//    // Estados internos
//    private val _currentUserData = MutableStateFlow<UserData?>(null)
//    val currentUserData: StateFlow<UserData?> = _currentUserData.asStateFlow()
//
//    private val _currentScope = MutableStateFlow<DataScope>(DataScope.Personal(""))
//    val currentScope: StateFlow<DataScope> = _currentScope.asStateFlow()
//
//    /**
//     * Datos del usuario actual con información de permisos
//     */
//    data class UserData(
//        val uid: String,
//        val email: String,
//        val displayName: String?,
//        val planUsuario: PlanUsuario,
//        val empresaId: String?,
//        val tipoUsuario: TipoUsuario?,
//        val activo: Boolean = true
//    )
//
//    /**
//     * Planes de usuario
//     */
//    enum class PlanUsuario {
//        FREE,
//        PREMIUM
//    }
//
//    /**
//     * Tipos de usuario (solo para Premium)
//     */
//    enum class TipoUsuario {
//        SUPER_ADMIN,  // Creador de empresa, todos los permisos
//        ADMIN,        // Administrador, mayoría de permisos
//        EMPLEADO,     // Usuario normal, permisos específicos
//        INVITADO      // Solo lectura
//    }
//
//    /**
//     * Scope de datos: Personal (FREE) o Empresa (PREMIUM)
//     */
//    sealed class DataScope {
//        data class Personal(val userId: String) : DataScope()
//        data class Empresa(val empresaId: String) : DataScope()
//    }
//
//    /**
//     * Inicializar UserManager con usuario de Firebase Auth
//     */
//    fun initialize(firebaseUser: FirebaseUser?) {
//        Log.d(TAG, "Inicializando UserManager con usuario: ${firebaseUser?.email}")
//
//        if (firebaseUser == null) {
//            // No hay usuario autenticado
//            _currentUserData.value = null
//            _currentScope.value = DataScope.Personal("")
//            return
//        }
//
//        // Por ahora, asumir que todos los usuarios son FREE
//        // En FASE 1B cargaremos datos desde Firestore
//        val userData = UserData(
//            uid = firebaseUser.uid,
//            email = firebaseUser.email ?: "",
//            displayName = firebaseUser.displayName,
//            planUsuario = PlanUsuario.FREE, // TODO: Cargar desde Firestore
//            empresaId = null, // TODO: Cargar desde Firestore
//            tipoUsuario = null // Solo para Premium
//        )
//
//        _currentUserData.value = userData
//        updateScope(userData)
//
//        Log.d(TAG, "UserManager inicializado: ${userData.email} (${userData.planUsuario})")
//    }
//
//    /**
//     * Actualizar scope basado en datos del usuario
//     */
//    private fun updateScope(userData: UserData) {
//        _currentScope.value = when (userData.planUsuario) {
//            PlanUsuario.FREE -> DataScope.Personal(userData.uid)
//            PlanUsuario.PREMIUM -> {
//                if (userData.empresaId != null) {
//                    DataScope.Empresa(userData.empresaId)
//                } else {
//                    // Fallback si hay error en datos Premium
//                    DataScope.Personal(userData.uid)
//                }
//            }
//        }
//
//        Log.d(TAG, "Scope actualizado: ${_currentScope.value}")
//    }
//
//    /**
//     * Obtener scope actual para queries de datos
//     */
//    fun getCurrentScope(): DataScope {
//        return _currentScope.value
//    }
//
//    /**
//     * Verificar si el usuario actual es Premium
//     */
//    fun isPremium(): Boolean {
//        return _currentUserData.value?.planUsuario == PlanUsuario.PREMIUM
//    }
//
//    /**
//     * Verificar si el usuario actual es FREE
//     */
//    fun isFree(): Boolean {
//        return _currentUserData.value?.planUsuario == PlanUsuario.FREE
//    }
//
//    /**
//     * Obtener ID de empresa (solo Premium)
//     */
//    fun getEmpresaId(): String? {
//        return _currentUserData.value?.empresaId
//    }
//
//    /**
//     * Obtener tipo de usuario actual (solo Premium)
//     */
//    fun getTipoUsuario(): TipoUsuario? {
//        return _currentUserData.value?.tipoUsuario
//    }
//
//    /**
//     * Verificar si el usuario es Super Admin
//     */
//    fun isSuperAdmin(): Boolean {
//        return _currentUserData.value?.tipoUsuario == TipoUsuario.SUPER_ADMIN
//    }
//
//    /**
//     * Verificar si el usuario puede gestionar otros usuarios
//     */
//    fun canManageUsers(): Boolean {
//        val tipoUsuario = getTipoUsuario()
//        return tipoUsuario == TipoUsuario.SUPER_ADMIN || tipoUsuario == TipoUsuario.ADMIN
//    }
//
//    /**
//     * Obtener información del usuario actual
//     */
//    fun getCurrentUser(): UserData? {
//        return _currentUserData.value
//    }
//
//    /**
//     * Verificar si hay un usuario autenticado
//     */
//    fun isAuthenticated(): Boolean {
//        return _currentUserData.value != null
//    }
//
//    /**
//     * Limpiar datos del usuario (logout)
//     */
//    fun clearUserData() {
//        Log.d(TAG, "Limpiando datos de usuario")
//        _currentUserData.value = null
//        _currentScope.value = DataScope.Personal("")
//    }
//
//    /**
//     * Debug: Mostrar estado actual
//     */
//    fun debugCurrentState(): String {
//        val user = _currentUserData.value
//        val scope = _currentScope.value
//
//        return """
//            UserManager Estado:
//            - Usuario: ${user?.email ?: "null"}
//            - Plan: ${user?.planUsuario ?: "null"}
//            - Tipo: ${user?.tipoUsuario ?: "null"}
//            - Empresa: ${user?.empresaId ?: "null"}
//            - Scope: $scope
//            - Autenticado: ${isAuthenticated()}
//        """.trimIndent()
//    }
//}