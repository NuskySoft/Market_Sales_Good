
package es.nuskysoftware.marketsales.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import es.nuskysoftware.marketsales.utils.ConfigurationManager

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthResult {
    data class Success(val user: FirebaseUser?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    companion object {
        private const val TAG = "AuthRepository"
        private const val USERS_COLLECTION = "usuarios"
    }

    init {
        // Inicializar estado basado en usuario actual
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        _authState.value = if (user != null) {
            AuthState.Authenticated(user)
        } else {
            AuthState.Unauthenticated
        }

        // Listener para cambios de autenticación
        firebaseAuth.addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            _currentUser.value = currentUser
            _authState.value = if (currentUser != null) {
                AuthState.Authenticated(currentUser)
            } else {
                AuthState.Unauthenticated
            }
        }

        Log.d(TAG, "AuthRepository inicializado - Usuario actual: ${user?.email ?: "null"}")
    }

    // ✅ FASE 1 - Funciones de testing (ya funcionando)
    fun testConnection(): Boolean {
        return try {
            val app = firebaseAuth.app
            Log.d(TAG, "Conexión Firebase Auth OK - App: ${app.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error conexión Firebase Auth", e)
            false
        }
    }

    fun isUserAuthenticated(): Boolean {
        val authenticated = firebaseAuth.currentUser != null
        Log.d(TAG, "¿Usuario autenticado? $authenticated")
        return authenticated
    }

    // 🔐 FASE 2 - IMPLEMENTACIÓN EMAIL/PASSWORD

    /**
     * Registrar nuevo usuario con email y contraseña
     */
    suspend fun registerWithEmail(email: String, password: String): AuthResult {
        return try {
            Log.d(TAG, "Iniciando registro para: $email")
            _authState.value = AuthState.Loading

            // Crear usuario en Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                Log.d(TAG, "Usuario registrado exitosamente: ${user.email}")

                // ✅ CORREGIDO: Crear documento del usuario con configuración por defecto
                createUserDocumentWithDefaults(user)

                // ✅ CORREGIDO: Cargar configuración SIN resetear
                loadUserConfigurationWithoutReset(user.uid)

                _authState.value = AuthState.Authenticated(user)
                AuthResult.Success(user)
            } else {
                val error = "Error: Usuario nulo después del registro"
                Log.e(TAG, error)
                _authState.value = AuthState.Error(error)
                AuthResult.Error(error)
            }

        } catch (e: Exception) {
            val errorMessage = "Error en registro: ${e.message}"
            Log.e(TAG, errorMessage, e)
            _authState.value = AuthState.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    /**
     * Iniciar sesión con email y contraseña
     */
    suspend fun loginWithEmail(email: String, password: String): AuthResult {
        return try {
            Log.d(TAG, "Iniciando login para: $email")
            _authState.value = AuthState.Loading

            // Autenticar con Firebase Auth
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                Log.d(TAG, "Login exitoso: ${user.email}")

                // ✅ CORREGIDO: Cargar configuración SIN resetear
                loadUserConfigurationWithoutReset(user.uid)

                _authState.value = AuthState.Authenticated(user)
                AuthResult.Success(user)
            } else {
                val error = "Error: Usuario nulo después del login"
                Log.e(TAG, error)
                _authState.value = AuthState.Error(error)
                AuthResult.Error(error)
            }

        } catch (e: Exception) {
            val errorMessage = when (e.message) {
                "The email address is badly formatted." -> "Email inválido"
                "The password is invalid or the user does not have a password." -> "Contraseña incorrecta"
                "There is no user record corresponding to this identifier. The user may have been deleted." -> "Usuario no encontrado"
                "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Error de conexión"
                else -> "Error en login: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            _authState.value = AuthState.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    // 🚀 FASE 3 - GOOGLE AUTH - CORREGIDO
    /**
     * Iniciar sesión con Google usando idToken
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            Log.d(TAG, "Iniciando Google Auth con idToken")
            _authState.value = AuthState.Loading

            // Crear credencial de Google con el idToken
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // Autenticar con Firebase usando la credencial de Google
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                Log.d(TAG, "Google Auth exitoso: ${user.email}")
                Log.d(TAG, "Usuario Google - Name: ${user.displayName}, Photo: ${user.photoUrl}")

                // ✅ CORREGIDO: Crear o actualizar documento SIN resetear configuración existente
                createUserDocumentIfNotExists(user)

                // ✅ CORREGIDO: Cargar configuración SIN resetear - ESTE ERA EL PROBLEMA
                loadUserConfigurationWithoutReset(user.uid)

                _authState.value = AuthState.Authenticated(user)
                AuthResult.Success(user)
            } else {
                val error = "Error: Usuario nulo después de Google Auth"
                Log.e(TAG, error)
                _authState.value = AuthState.Error(error)
                AuthResult.Error(error)
            }

        } catch (e: Exception) {
            val errorMessage = when (e.message) {
                "An internal error has occurred. [ INVALID_IDP_RESPONSE ]" -> "Error de autenticación con Google"
                "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Error de conexión"
                else -> "Error en Google Auth: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            _authState.value = AuthState.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    /**
     * Cerrar sesión - ✅ CORREGIDO: Cargar configuración usuario_default
     */
    suspend fun logout(): AuthResult {
        return try {
            Log.d(TAG, "Cerrando sesión...")
            _authState.value = AuthState.Loading

            // Cerrar sesión en Firebase
            firebaseAuth.signOut()

            // ✅ CORREGIDO: Cargar configuración de usuario_default EN LUGAR de resetear
            loadDefaultUserConfiguration()

            Log.d(TAG, "Sesión cerrada exitosamente")
            _authState.value = AuthState.Unauthenticated

            AuthResult.Success(null)

        } catch (e: Exception) {
            val errorMessage = "Error al cerrar sesión: ${e.message}"
            Log.e(TAG, errorMessage, e)
            _authState.value = AuthState.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    /**
     * ✅ NUEVO: Crear documento de usuario con configuración por defecto (solo para nuevos usuarios)
     */
    private suspend fun createUserDocumentWithDefaults(user: FirebaseUser) {
        try {
            // Para usuarios nuevos, usar configuración por defecto
            val configMap = hashMapOf<String, Any>(
                "idioma" to "es",
                "fuente" to "Montserrat",
                "modoOscuro" to false
            )

            val userDoc = hashMapOf<String, Any>(
                "email" to (user.email ?: ""),
                "uid" to user.uid,
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "fechaCreacion" to System.currentTimeMillis(),
                "isPremium" to false,
                "configuracion" to configMap,
                "provider" to if (user.providerData.any { it.providerId == "google.com" }) "google" else "email"
            )

            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(userDoc)
                .await()

            Log.d(TAG, "Documento de usuario creado con configuración por defecto")

        } catch (e: Exception) {
            Log.e(TAG, "Error creando documento de usuario", e)
        }
    }

    /**
     * ✅ NUEVO: Crear documento solo si no existe (para usuarios existentes de Google)
     */
    private suspend fun createUserDocumentIfNotExists(user: FirebaseUser) {
        try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .get()
                .await()

            if (!document.exists()) {
                // Solo crear si no existe
                createUserDocumentWithDefaults(user)
                Log.d(TAG, "Documento de usuario Google creado (primera vez)")
            } else {
                // Actualizar solo información básica, NO configuración
                val updates = hashMapOf<String, Any>(
                    "displayName" to (user.displayName ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "provider" to "google"
                )

                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .update(updates)
                    .await()

                Log.d(TAG, "Información básica de usuario Google actualizada (preservando configuración)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creando/actualizando documento de usuario", e)
        }
    }

    /**
     * ✅ NUEVO: Cargar configuración SIN resetear (el método que FUNCIONABA para Email)
     */
    private suspend fun loadUserConfigurationWithoutReset(userId: String) {
        try {
            Log.d(TAG, "Cargando configuración para usuario: $userId (SIN resetear)")

            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val config = document.get("configuracion") as? Map<String, Any>
                val isPremium = document.getBoolean("isPremium") ?: false

                if (config != null) {
                    // ✅ APLICAR configuración del usuario SIN resetear primero
                    ConfigurationManager.updateConfiguration(
                        idioma = config["idioma"] as? String ?: "es",
                        fuente = config["fuente"] as? String ?: "Montserrat",
                        modoOscuro = config["modoOscuro"] as? Boolean ?: false,
                        isPremium = isPremium
                    )
                    Log.d(TAG, "✅ Configuración de usuario cargada: idioma=${config["idioma"]}, fuente=${config["fuente"]}, tema=${config["modoOscuro"]}, premium=$isPremium")
                } else {
                    Log.d(TAG, "No se encontró configuración específica, manteniendo actual")
                }
            } else {
                Log.d(TAG, "Documento de usuario no existe, creando configuración por defecto")
                createUserDocumentWithDefaults(firebaseAuth.currentUser!!)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando configuración de usuario", e)
            // NO resetear en caso de error, mantener configuración actual
        }
    }

    /**
     * ✅ CORREGIDO: Cargar configuración de usuario_default desde tabla CONFIGURACIONES
     */
    private suspend fun loadDefaultUserConfiguration() {
        try {
            Log.d(TAG, "Cargando configuración de usuario_default desde tabla CONFIGURACIONES")

            // ✅ CORREGIDO: Buscar en colección "configuraciones", NO en "usuarios"
            val document = firestore.collection("configuraciones")
                .document("usuario_default")
                .get()
                .await()

            if (document.exists()) {
                // ✅ CORREGIDO: Leer directamente los campos (no están en sub-objeto "configuracion")
                val idioma = document.getString("idioma") ?: "es"
                val fuente = document.getString("fuente") ?: "Montserrat"
                val temaOscuro = document.getBoolean("temaOscuro") ?: false
                val versionApp = document.getLong("versionApp")?.toInt() ?: 0
                val isPremium = versionApp == 1

                // Aplicar configuración de usuario_default
                ConfigurationManager.updateConfiguration(
                    idioma = idioma,
                    fuente = fuente,
                    modoOscuro = temaOscuro,
                    isPremium = isPremium
                )

                Log.d(TAG, "✅ Configuración usuario_default cargada desde CONFIGURACIONES:")
                Log.d(TAG, "   - idioma: $idioma")
                Log.d(TAG, "   - fuente: $fuente")
                Log.d(TAG, "   - temaOscuro: $temaOscuro")
                Log.d(TAG, "   - versionApp: $versionApp (isPremium: $isPremium)")

            } else {
                Log.d(TAG, "No existe usuario_default en configuraciones, creando configuración por defecto")
                createDefaultUserConfigurationInConfigTable()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando configuración usuario_default desde configuraciones", e)
            // En caso de error, resetear a valores por defecto
            ConfigurationManager.resetToDefaults()
        }
    }

    /**
     * ✅ CORREGIDO: Crear configuración usuario_default en tabla CONFIGURACIONES
     */
    private suspend fun createDefaultUserConfigurationInConfigTable() {
        try {
            Log.d(TAG, "Creando usuario_default en tabla CONFIGURACIONES")

            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

            val defaultConfigDoc = hashMapOf<String, Any>(
                "versionApp" to 0,
                "numeroVersion" to "V1.0",
                "ultimoDispositivo" to android.os.Build.MODEL,
               "usuarioEmail" to "null",
                "usuarioId" to "usuario_default",
                "usuarioPassword" to "null",
                "idioma" to "es",
                "temaOscuro" to false,
                "fuente" to "Montserrat",
                "moneda" to "€ Euro",
                "fechaUltimaSync" to dateFormat.format(java.util.Date()),
                "pendienteSync" to false
            )

            firestore.collection("configuraciones")
                .document("usuario_default")
                .set(defaultConfigDoc)
                .await()

            // Aplicar la configuración por defecto
            ConfigurationManager.resetToDefaults()

            Log.d(TAG, "✅ Usuario_default creado en tabla CONFIGURACIONES")

        } catch (e: Exception) {
            Log.e(TAG, "Error creando usuario_default en configuraciones", e)
            ConfigurationManager.resetToDefaults()
        }
    }

    /**
     * Actualizar configuración del usuario en Firestore
     */
    suspend fun updateUserConfiguration(
        idioma: String,
        fuente: String,
        modoOscuro: Boolean,
        isPremium: Boolean
    ): Boolean {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: return false

            val configUpdate = mapOf(
                "configuracion.idioma" to idioma,
                "configuracion.fuente" to fuente,
                "configuracion.modoOscuro" to modoOscuro,
                "isPremium" to isPremium
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(configUpdate)
                .await()

            Log.d(TAG, "Configuración de usuario actualizada en Firestore")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando configuración", e)
            false
        }
    }

    /**
     * Obtener información del usuario actual
     */
    fun getCurrentUserInfo(): Map<String, Any>? {
        val user = firebaseAuth.currentUser
        return if (user != null) {
            mapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "isEmailVerified" to user.isEmailVerified,
                "creationTimestamp" to (user.metadata?.creationTimestamp ?: 0L),
                "provider" to if (user.providerData.any { it.providerId == "google.com" }) "google" else "email"
            )
        } else null
    }
}
