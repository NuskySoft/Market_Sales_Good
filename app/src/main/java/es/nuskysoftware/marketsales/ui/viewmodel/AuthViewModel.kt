package es.nuskysoftware.marketsales.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import es.nuskysoftware.marketsales.data.repository.AuthRepository
import es.nuskysoftware.marketsales.data.repository.AuthResult
import es.nuskysoftware.marketsales.data.repository.AuthState
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    // Estados expuestos
    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    init {
        Log.d(TAG, "AuthViewModel inicializado")

        // Testing inicial (mantener para verificar funcionamiento)
        testAuthConnection()
    }

    // ✅ FASE 1 - Funciones de testing (mantener)
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

    // 🔐 FASE 2 - FUNCIONES DE AUTENTICACIÓN CON SAFE CALLS

    /**
     * Registrar nuevo usuario con email y contraseña
     */
    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando registro para $email")

            try {
                when (val result = authRepository.registerWithEmail(email, password)) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "ViewModel: Registro exitoso - ${result.user?.email ?: "sin email"}")
                        // El estado se actualiza automáticamente vía StateFlow
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "ViewModel: Error en registro - ${result.message}")
                        // El estado de error se maneja automáticamente
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en registro", e)
            }
        }
    }

    /**
     * Iniciar sesión con email y contraseña
     */
    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando login para $email")

            try {
                when (val result = authRepository.loginWithEmail(email, password)) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "ViewModel: Login exitoso - ${result.user?.email ?: "sin email"}")
                        // El estado se actualiza automáticamente vía StateFlow
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "ViewModel: Error en login - ${result.message}")
                        // El estado de error se maneja automáticamente
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en login", e)
            }
        }
    }

    // 🚀 FASE 3 - GOOGLE AUTH V8 - NUEVO MÉTODO IMPLEMENTADO
    /**
     * Iniciar sesión con Google usando idToken
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando Google Auth")

            try {
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "ViewModel: Google Auth exitoso - ${result.user?.email ?: "sin email"}")
                        Log.d(TAG, "ViewModel: Usuario Google - ${result.user?.displayName ?: "sin nombre"}")
                        // El estado se actualiza automáticamente vía StateFlow
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "ViewModel: Error en Google Auth - ${result.message}")
                        // El estado de error se maneja automáticamente
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en Google Auth", e)
            }
        }
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Iniciando logout")

            try {
                when (val result = authRepository.logout()) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "ViewModel: Logout exitoso")
                        // El estado se actualiza automáticamente vía StateFlow
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "ViewModel: Error en logout - ${result.message}")
                        // El estado de error se maneja automáticamente
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: Excepción no controlada en logout", e)
            }
        }
    }

    /**
     * Verificar si el usuario está autenticado
     */
    fun isUserAuthenticated(): Boolean {
        return authRepository.isUserAuthenticated()
    }

    /**
     * Limpiar errores de autenticación
     */
    fun clearAuthError() {
        Log.d(TAG, "ViewModel: Limpiando errores de autenticación")
    }
}

//package es.nuskysoftware.marketsales.ui.viewmodel
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import es.nuskysoftware.marketsales.data.repository.AuthRepository
//import es.nuskysoftware.marketsales.data.repository.AuthResult
//import es.nuskysoftware.marketsales.data.repository.AuthState
//import com.google.firebase.auth.FirebaseUser
//
//class AuthViewModel(
//    private val authRepository: AuthRepository
//) : ViewModel() {
//
//    companion object {
//        private const val TAG = "AuthViewModel"
//    }
//
//    // Estados expuestos
//    val authState: StateFlow<AuthState> = authRepository.authState
//    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser
//
//    init {
//        Log.d(TAG, "AuthViewModel inicializado")
//
//        // Testing inicial (mantener para verificar funcionamiento)
//        testAuthConnection()
//    }
//
//    // ✅ FASE 1 - Funciones de testing (mantener)
//    private fun testAuthConnection() {
//        viewModelScope.launch {
//            try {
//                val connectionTest = authRepository.testConnection()
//                val isAuthenticated = authRepository.isUserAuthenticated()
//                val user = currentUser.value
//
//                Log.d(TAG, "✅ DEBUG AuthViewModel - Test resultados:")
//                Log.d(TAG, "   - Conexión Firebase: $connectionTest")
//                Log.d(TAG, "   - Usuario autenticado: $isAuthenticated")
//                Log.d(TAG, "   - Usuario actual: ${user?.email ?: "null"}")
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error en test de conexión", e)
//            }
//        }
//    }
//
//    // 🔐 FASE 2 - FUNCIONES DE AUTENTICACIÓN CON SAFE CALLS
//
//    /**
//     * Registrar nuevo usuario con email y contraseña
//     */
//    fun registerWithEmail(email: String, password: String) {
//        viewModelScope.launch {
//            Log.d(TAG, "ViewModel: Iniciando registro para $email")
//
//            try {
//                when (val result = authRepository.registerWithEmail(email, password)) {
//                    is AuthResult.Success -> {
//                        Log.d(TAG, "ViewModel: Registro exitoso - ${result.user?.email ?: "sin email"}")
//                        // El estado se actualiza automáticamente vía StateFlow
//                    }
//                    is AuthResult.Error -> {
//                        Log.e(TAG, "ViewModel: Error en registro - ${result.message}")
//                        // El estado de error se maneja automáticamente
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "ViewModel: Excepción no controlada en registro", e)
//            }
//        }
//    }
//
//    /**
//     * Iniciar sesión con email y contraseña
//     */
//    fun loginWithEmail(email: String, password: String) {
//        viewModelScope.launch {
//            Log.d(TAG, "ViewModel: Iniciando login para $email")
//
//            try {
//                when (val result = authRepository.loginWithEmail(email, password)) {
//                    is AuthResult.Success -> {
//                        Log.d(TAG, "ViewModel: Login exitoso - ${result.user?.email ?: "sin email"}")
//                        // El estado se actualiza automáticamente vía StateFlow
//                    }
//                    is AuthResult.Error -> {
//                        Log.e(TAG, "ViewModel: Error en login - ${result.message}")
//                        // El estado de error se maneja automáticamente
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "ViewModel: Excepción no controlada en login", e)
//            }
//        }
//    }
//
//    /**
//     * Cerrar sesión
//     */
//    fun logout() {
//        viewModelScope.launch {
//            Log.d(TAG, "ViewModel: Iniciando logout")
//
//            try {
//                when (val result = authRepository.logout()) {
//                    is AuthResult.Success -> {
//                        Log.d(TAG, "ViewModel: Logout exitoso")
//                        // El estado se actualiza automáticamente vía StateFlow
//                    }
//                    is AuthResult.Error -> {
//                        Log.e(TAG, "ViewModel: Error en logout - ${result.message}")
//                        // El estado de error se maneja automáticamente
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "ViewModel: Excepción no controlada en logout", e)
//            }
//        }
//    }
//
//    /**
//     * Verificar si el usuario está autenticado
//     */
//    fun isUserAuthenticated(): Boolean {
//        return authRepository.isUserAuthenticated()
//    }
//
//    /**
//     * Limpiar errores de autenticación
//     */
//    fun clearAuthError() {
//        Log.d(TAG, "ViewModel: Limpiando errores de autenticación")
//    }
//}