// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/AuthViewModel.kt
package es.nuskysoftware.marketsales.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseUser
import es.nuskysoftware.marketsales.data.repository.AuthRepository
import es.nuskysoftware.marketsales.data.repository.AuthResult
import es.nuskysoftware.marketsales.data.repository.AuthState
import es.nuskysoftware.marketsales.data.repository.SyncState
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val authRepository: AuthRepository,
    // ✅ Inyectamos también el repo de mercadillos para recalcular estados al finalizar restauración/sync
    private val mercadilloRepository: MercadilloRepository
) : ViewModel() {

    companion object { private const val TAG = "AuthViewModel" }

    private val firebaseAuth = FirebaseAuth.getInstance()

    // Estados expuestos
    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    // Restauración Premium
    val restoreAllowed: StateFlow<Boolean?> get() = authRepository.restoreAllowed
    val restoreBlockMessage: StateFlow<String?> get() = authRepository.restoreBlockMessage

    // ✅ Estado de sincronización y progreso (passthrough al repo)
    val syncState: StateFlow<SyncState> get() = authRepository.syncState
    val downloadProgress: StateFlow<Int> get() = authRepository.downloadProgress
    val downloadMessage: StateFlow<String> get() = authRepository.downloadMessage

    init {
        Log.d(TAG, "AuthViewModel inicializado")
        testAuthConnection()

        // ✅ Cuando termina una descarga/merge (Done), recalculemos estados automáticos
        viewModelScope.launch {
            syncState.collectLatest { state ->
                if (state == SyncState.Done) {
                    val uid = ConfigurationManager.getCurrentUserId()
                    if (!uid.isNullOrBlank() && uid != "usuario_default") {
                        try {
                            Log.d(TAG, "🔄 Sync Done → recalculando estados automáticos para $uid…")
                            mercadilloRepository.actualizarEstadosAutomaticos(uid)
                            Log.d(TAG, "✅ Recalculo de estados completado")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error recalculando estados tras sync", e)
                        }
                    } else {
                        Log.d(TAG, "ℹ️ Sync Done pero sin usuario válido; se omite recalculo de estados")
                    }
                }
            }
        }
    }

    private fun testAuthConnection() {
        viewModelScope.launch {
            try {
                val connectionTest = authRepository.testConnection()
                val isAuthenticated = authRepository.isUserAuthenticated()
                val user = currentUser.value
                Log.d(TAG, "✅ DEBUG AuthViewModel - Test resultados:")
                Log.d(TAG, "   - Conexión Firebase: $connectionTest")
                Log.d(TAG, "   - Usuario autenticado: $isAuthenticated")
                Log.d(TAG, "   - Usuario actual: ${user?.email ?: "null"}")
            } catch (e: Exception) {
                Log.e(TAG, "Error en test de conexión", e)
            }
        }
    }

    // ---------- AUTENTICACIÓN ----------
    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando registro para $email")
            try {
                when (val result = authRepository.registerWithEmail(email, password)) {
                    is AuthResult.Success -> Log.d(TAG, "ViewModel: Registro ok - ${result.user?.email}")
                    is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error en registro - ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en registro", e)
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando login para $email")
            try {
                when (val result = authRepository.loginWithEmail(email, password)) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "ViewModel: Login ok - ${result.user?.email}")
                        val uid = ConfigurationManager.getCurrentUserId()
                        if (!uid.isNullOrBlank()) {
                            viewModelScope.launch { mercadilloRepository.actualizarEstadosAutomaticos(uid) }
                        }
                    }
                    is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error en login - ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en login", e)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando Google Auth")
            try {
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "ViewModel: Google ok - ${result.user?.email}")
                        Log.d(TAG, "ViewModel: Nombre - ${result.user?.displayName}")

                        // Igual que en loginWithEmail: recalcular estados tras autenticación
                        val uid = result.user?.uid ?: ConfigurationManager.getCurrentUserId()
                        if (!uid.isNullOrBlank()) {
                            viewModelScope.launch { mercadilloRepository.actualizarEstadosAutomaticos(uid) }
                        }
                    }
                    is AuthResult.Error -> Log.e(TAG, "ViewModel: Error Google - ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en Google Auth", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando logout")
            try {
                when (val result = authRepository.logout()) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "ViewModel: Logout ok")
                        val uid = ConfigurationManager.getCurrentUserId()
                        if (!uid.isNullOrBlank()) {
                            viewModelScope.launch { mercadilloRepository.actualizarEstadosAutomaticos(uid) }
                        }
                    }
                    is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error logout - ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en logout", e)
            }
        }
    }

    fun isUserAuthenticated(): Boolean = authRepository.isUserAuthenticated()

    fun clearAuthError() {
        Log.d(TAG, "ViewModel: Limpiando errores de autenticación")
    }

    // ---------- PERFIL ----------
    suspend fun updateUserProfile(displayName: String, email: String): Boolean {
        return try {
            val user = firebaseAuth.currentUser ?: return false

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            user.updateProfile(profileUpdates).await()
            user.reload().await()

            if (user.email != email) { user.updateEmail(email).await() }

            authRepository.updateUserProfileAndMarkDirty(user.uid, displayName, email)

            ConfigurationManager.updateUserConfiguration(
                displayName = displayName,
                usuarioEmail = email,
                isAuthenticated = true
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando perfil", e)
            false
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Boolean {
        return try {
            val user = firebaseAuth.currentUser ?: return false
            val email = user.email ?: return false
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Log.d(TAG, "✅ Contraseña actualizada correctamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cambiando contraseña", e)
            false
        }
    }

    // ---------- PREMIUM / RESTAURACIÓN ----------
    fun upgradeToPremiumAndRestore() {
        viewModelScope.launch {
            try {
                val ok = authRepository.updateUserPremium(true)
                if (ok) {
                    authRepository.refreshUserConfiguration()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al actualizar a Premium y restaurar", e)
            }
        }
    }
    suspend fun shouldShowDefaultUserWarning(): Boolean {
        return authRepository.shouldShowDefaultUserWarning()
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/viewmodel/AuthViewModel.kt
//package es.nuskysoftware.marketsales.ui.viewmodel
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.firebase.auth.EmailAuthProvider
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.UserProfileChangeRequest
//import com.google.firebase.auth.FirebaseUser
//import es.nuskysoftware.marketsales.data.repository.AuthRepository
//import es.nuskysoftware.marketsales.data.repository.AuthResult
//import es.nuskysoftware.marketsales.data.repository.AuthState
//import es.nuskysoftware.marketsales.data.repository.SyncState
//import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.flow.collectLatest
//
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.launch
//
//
//
//class AuthViewModel(
//    private val authRepository: AuthRepository,
//    // ✅ Inyectamos también el repo de mercadillos para recalcular estados al finalizar restauración/sync
//    private val mercadilloRepository: MercadilloRepository
//) : ViewModel() {
//
//    companion object { private const val TAG = "AuthViewModel" }
//
//    private val firebaseAuth = FirebaseAuth.getInstance()
//
//    // Estados expuestos
//    val authState: StateFlow<AuthState> = authRepository.authState
//    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser
//
//    // Restauración Premium
//    val restoreAllowed: StateFlow<Boolean?> get() = authRepository.restoreAllowed
//    val restoreBlockMessage: StateFlow<String?> get() = authRepository.restoreBlockMessage
//
//    // ✅ Estado de sincronización y progreso (passthrough al repo)
//    val syncState: StateFlow<SyncState> get() = authRepository.syncState
//    val downloadProgress: StateFlow<Int> get() = authRepository.downloadProgress
//    val downloadMessage: StateFlow<String> get() = authRepository.downloadMessage
//
//    init {
//        Log.d(TAG, "AuthViewModel inicializado")
//        testAuthConnection()
//
//        // ✅ Cuando termina una descarga/merge (Done), recalculemos estados automáticos
//        viewModelScope.launch {
//            syncState.collectLatest { state ->
//                if (state == SyncState.Done) {
//                    val uid = ConfigurationManager.getCurrentUserId()
//                    if (!uid.isNullOrBlank() && uid != "usuario_default") {
//                        try {
//                            Log.d(TAG, "🔄 Sync Done → recalculando estados automáticos para $uid…")
//                            mercadilloRepository.actualizarEstadosAutomaticos(uid)
//                            Log.d(TAG, "✅ Recalculo de estados completado")
//                        } catch (e: Exception) {
//                            Log.e(TAG, "❌ Error recalculando estados tras sync", e)
//                        }
//                    } else {
//                        Log.d(TAG, "ℹ️ Sync Done pero sin usuario válido; se omite recalculo de estados")
//                    }
//                }
//            }
//        }
//    }
//
//    private fun testAuthConnection() {
//        viewModelScope.launch {
//            try {
//                val connectionTest = authRepository.testConnection()
//                val isAuthenticated = authRepository.isUserAuthenticated()
//                val user = currentUser.value
//                Log.d(TAG, "✅ DEBUG AuthViewModel - Test resultados:")
//                Log.d(TAG, "   - Conexión Firebase: $connectionTest")
//                Log.d(TAG, "   - Usuario autenticado: $isAuthenticated")
//                Log.d(TAG, "   - Usuario actual: ${user?.email ?: "null"}")
//            } catch (e: Exception) {
//                Log.e(TAG, "Error en test de conexión", e)
//            }
//        }
//    }
//
//    // ---------- AUTENTICACIÓN ----------
//    fun registerWithEmail(email: String, password: String) {
//        viewModelScope.launch {
//            Log.d(TAG, "ViewModel: Iniciando registro para $email")
//            try {
//                when (val result = authRepository.registerWithEmail(email, password)) {
//                    is AuthResult.Success -> Log.d(TAG, "ViewModel: Registro ok - ${result.user?.email}")
//                    is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error en registro - ${result.message}")
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "ViewModel: Excepción no controlada en registro", e)
//            }
//        }
//    }
//
//    fun loginWithEmail(email: String, password: String) {
//        viewModelScope.launch {
//            Log.d(TAG, "ViewModel: Iniciando login para $email")
//            try {
//                when (val result = authRepository.loginWithEmail(email, password)) {
////                    is AuthResult.Success -> Log.d(TAG, "ViewModel: Login ok - ${result.user?.email}")
//                    is AuthResult.Success -> {
//                        Log.d(TAG, "ViewModel: Login ok - ${result.user?.email}")
//                        val uid = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId()
//                        if (!uid.isNullOrBlank()) {
//                            viewModelScope.launch { mercadilloRepository.actualizarEstadosAutomaticos(uid) }
//                        }
//                    }
//                    is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error en login - ${result.message}")
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "ViewModel: Excepción no controlada en login", e)
//            }
//        }
//    }
//
//    fun signInWithGoogle(idToken: String) {
//        viewModelScope.launch {
//            Log.d(TAG, "ViewModel: Iniciando Google Auth")
//            try {
//                when (val result = authRepository.signInWithGoogle(idToken)) {
//                    is AuthResult.Success -> {
//                        Log.d(TAG, "ViewModel: Google ok - ${result.user?.email}")
//                        Log.d(TAG, "ViewModel: Nombre - ${result.user?.displayName}")
//
//                        fun signInWithGoogle(idToken: String) {
//                            viewModelScope.launch {
//                                Log.d(TAG, "ViewModel: Iniciando Google Auth")
//                                try {
//                                    when (val result = authRepository.signInWithGoogle(idToken)) {
//                                        is AuthResult.Success -> {
//                                            Log.d(TAG, "ViewModel: Google ok - ${result.user?.email}")
//                                            Log.d(TAG, "ViewModel: Nombre - ${result.user?.displayName}")
//
//                                            // ✅ Igual que en loginWithEmail: recalcular estados tras autenticación
//                                            val uid = result.user?.uid ?: es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId()
//                                            if (!uid.isNullOrBlank()) {
//                                                viewModelScope.launch { mercadilloRepository.actualizarEstadosAutomaticos(uid) }
//                                            }
//                                        }
//                                        is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error Google - ${result.message}")
//                                    }
//                                } catch (e: Exception) {
//                                    Log.e(TAG, "ViewModel: Excepción no controlada en Google Auth", e)
//                                }
//                            }
//                        }
//                    }
//                    is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error Google - ${result.message}")
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "ViewModel: Excepción no controlada en Google Auth", e)
//            }
//        }
//    }
//
//    fun logout() {
//        viewModelScope.launch {
//            Log.d(TAG, "ViewModel: Iniciando logout")
//            try {
//                when (val result = authRepository.logout()) {
////                    is AuthResult.Success -> Log.d(TAG, "ViewModel: Logout ok")
//                    is AuthResult.Success -> {
//                        Log.d(TAG, "ViewModel: Logout ok")
//                        val uid = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId()
//                        if (!uid.isNullOrBlank()) {
//                            viewModelScope.launch { mercadilloRepository.actualizarEstadosAutomaticos(uid) }
//                        }
//                    }
//                    is AuthResult.Error   -> Log.e(TAG, "ViewModel: Error logout - ${result.message}")
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "ViewModel: Excepción no controlada en logout", e)
//            }
//        }
//    }
//
//    fun isUserAuthenticated(): Boolean = authRepository.isUserAuthenticated()
//
//    fun clearAuthError() {
//        Log.d(TAG, "ViewModel: Limpiando errores de autenticación")
//    }
//
//    // ---------- PERFIL ----------
//    suspend fun updateUserProfile(displayName: String, email: String): Boolean {
//        return try {
//            val user = firebaseAuth.currentUser ?: return false
//
//            val profileUpdates = UserProfileChangeRequest.Builder()
//                .setDisplayName(displayName)
//                .build()
//
//            user.updateProfile(profileUpdates).await()
//            user.reload().await()
//
//            if (user.email != email) { user.updateEmail(email).await() }
//
//            authRepository.updateUserProfileAndMarkDirty(user.uid, displayName, email)
//
//            ConfigurationManager.updateUserConfiguration(
//                displayName = displayName,
//                usuarioEmail = email,
//                isAuthenticated = true
//            )
//            true
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error actualizando perfil", e)
//            false
//        }
//    }
//
//    suspend fun updatePassword(currentPassword: String, newPassword: String): Boolean {
//        return try {
//            val user = firebaseAuth.currentUser ?: return false
//            val email = user.email ?: return false
//            val credential = EmailAuthProvider.getCredential(email, currentPassword)
//            user.reauthenticate(credential).await()
//            user.updatePassword(newPassword).await()
//            Log.d(TAG, "✅ Contraseña actualizada correctamente")
//            true
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error cambiando contraseña", e)
//            false
//        }
//    }
//
//    // ---------- PREMIUM / RESTAURACIÓN ----------
//    fun upgradeToPremiumAndRestore() {
//        viewModelScope.launch {
//            try {
//                val ok = authRepository.updateUserPremium(true)
//                if (ok) {
//                    authRepository.refreshUserConfiguration()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "❌ Error al actualizar a Premium y restaurar", e)
//            }
//        }
//    }
//    suspend fun shouldShowDefaultUserWarning(): Boolean {
//        return authRepository.shouldShowDefaultUserWarning()
//    }
//}
//
//
