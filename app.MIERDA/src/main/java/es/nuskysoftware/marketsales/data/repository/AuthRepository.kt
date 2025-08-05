// app/src/main/java/es/nuskysoftware/marketsales/data/repository/AuthRepository.kt
package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*
import es.nuskysoftware.marketsales.data.local.dao.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * AuthRepository — Login + restauración Premium con indicador de progreso (TOP-LEVEL Firestore)
 *
 * Política de descarga MASIVA: **solo** se descarga desde Firebase cuando Room está VACÍO.
 * Si Room NO está vacío:
 *   - Con pendientes locales → solo se SUBEN pendientes. (No se descarga).
 *   - Sin pendientes        → no se hace nada (se respetan datos locales).
 *
 * Clave: se trabaja con colecciones TOP-LEVEL:
 *  - categorias (userId == uid)
 *  - articulos (userId == uid)
 *  - mercadillos (userId == uid)
 *  - recibos (idUsuario == uid)
 *  - lineas_venta (idUsuario == uid)
 */

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

sealed class SyncState {
    object Idle : SyncState()
    object Checking : SyncState()
    object Downloading : SyncState()
    object Uploading : SyncState()
    object Merging : SyncState()
    object Done : SyncState()
    data class Error(val message: String) : SyncState()
}

class AuthRepository(
    private val context: Context
) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ===== Restauración Premium (UI) =====
    private val _restoreAllowed = MutableStateFlow<Boolean?>(null)
    val restoreAllowed: StateFlow<Boolean?> = _restoreAllowed
    private val _restoreBlockMessage = MutableStateFlow<String?>(null)
    val restoreBlockMessage: StateFlow<String?> = _restoreBlockMessage

    // ===== Estado de sincronización (UI) =====
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // ===== Progreso de descarga (UI) =====
    private val _downloadProgress = MutableStateFlow(0)         // 0..100
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()
    private val _downloadMessage = MutableStateFlow("")
    val downloadMessage: StateFlow<String> = _downloadMessage.asStateFlow()

    private fun setProgress(p: Int, msg: String) {
        val clamped = p.coerceIn(0, 100)
        _downloadProgress.value = clamped
        _downloadMessage.value = msg
        Log.d(TAG, "📶 Progreso descarga: $clamped% — $msg")
    }

    private var isLoadingConfiguration = false

    // Repos locales
    private val userRepository = UserRepository(context)
    private val configuracionRepository = ConfiguracionRepository(context)

    // Room
    private val db: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    private val categoriaDao: CategoriaDao by lazy { db.categoriaDao() }
    private val articuloDao: ArticuloDao by lazy { db.articuloDao() }
    private val mercadilloDao: MercadilloDao by lazy { db.mercadilloDao() }
    private val recibosDao: RecibosDao by lazy { db.recibosDao() }
    private val lineasDao: LineasVentaDao by lazy { db.lineasVentaDao() }

    // Auth + usuario actual
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Ámbito propio NO cancelable por recomposiciones
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AuthRepository"

        // Colecciones TOP-LEVEL (compat firma original)
        private fun colCategorias(db: FirebaseFirestore, @Suppress("UNUSED_PARAMETER") uid: String) =
            db.collection("categorias")
        private fun colArticulos(db: FirebaseFirestore, @Suppress("UNUSED_PARAMETER") uid: String) =
            db.collection("articulos")
        private fun colMercadillos(db: FirebaseFirestore, @Suppress("UNUSED_PARAMETER") uid: String) =
            db.collection("mercadillos")
        private fun colRecibos(db: FirebaseFirestore, @Suppress("UNUSED_PARAMETER") uid: String) =
            db.collection("recibos")
        private fun colLineasVenta(db: FirebaseFirestore, @Suppress("UNUSED_PARAMETER") uid: String) =
            db.collection("lineas_venta")
    }

    // ================== init ==================
    init {
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        _authState.value = if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated

        if (user != null) {
            Log.d(TAG, "Usuario existente detectado: ${user.email}")
            repoScope.launch {
                try {
                    loadUserConfigurationHybrid(user.uid)
                } catch (e: Exception) {
                    Log.e(TAG, "Error cargando configuración híbrida en init", e)
                    ConfigurationManager.updateUserConfiguration(
                        usuarioId = user.uid,
                        usuarioEmail = user.email,
                        displayName = user.displayName,
                        isAuthenticated = true,
                        planUsuario = "FREE"
                    )
                }
            }
        } else {
            ConfigurationManager.logout()
            Log.d(TAG, "No hay usuario - ConfigurationManager en estado logout")
        }

        firebaseAuth.addAuthStateListener { auth ->
            val current = auth.currentUser
            _currentUser.value = current
            _authState.value = if (current != null) AuthState.Authenticated(current) else AuthState.Unauthenticated
        }

        Log.d(TAG, "AuthRepository inicializado - Usuario actual: ${user?.email ?: "null"}")
    }

    // ========== TEST / ESTADO ==========
    fun testConnection(): Boolean = try {
        val app = firebaseAuth.app
        Log.d(TAG, "Conexión Firebase Auth OK - App: ${app.name}")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error conexión Firebase Auth", e); false
    }

    suspend fun refreshUserConfiguration() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "🔄 Forzando refresh de configuración de usuario")
            repoScope.launch { loadUserConfigurationHybrid(currentUser.uid) }
        }
    }

    fun isUserAuthenticated(): Boolean {
        val authenticated = firebaseAuth.currentUser != null
        Log.d(TAG, "¿Usuario autenticado? $authenticated")
        return authenticated
    }

    // ========== CARGA HÍBRIDA ==========
    private suspend fun loadUserConfigurationHybrid(usuarioUid: String) {
        if (isLoadingConfiguration) {
            Log.d(TAG, "Ya se está cargando configuración, omitiendo"); return
        }

        isLoadingConfiguration = true
        _syncState.value = SyncState.Checking
        setProgress(0, "Preparando…")

        try {
            Log.d(TAG, "🔄 Cargando configuración HÍBRIDA para usuario: $usuarioUid")

            val firebaseDisplayName = Firebase.auth.currentUser?.displayName
            val userEntity = userRepository.getHybridUserData(usuarioUid)
            val configEntity = configuracionRepository.getHybridConfiguracion()
            configuracionRepository.setUsuarioLogueado(usuarioUid)

            if (configEntity != null && userEntity != null) {
                val finalConfig = if (!userEntity.esPremium) {
                    configEntity.copy(idioma = "es", fuente = "Montserrat", moneda = "€ Euro", pendienteSync = true)
                } else configEntity

                val finalDisplayName =
                    if (!firebaseDisplayName.isNullOrBlank() && firebaseDisplayName != userEntity.displayName) {
                        userRepository.updateUserDisplayName(usuarioUid, firebaseDisplayName)
                        firebaseDisplayName
                    } else userEntity.displayName ?: ""

                ConfigurationManager.updateUserConfiguration(
                    idioma = finalConfig.idioma,
                    fuente = finalConfig.fuente,
                    temaOscuro = finalConfig.temaOscuro,
                    moneda = finalConfig.moneda,
                    usuarioEmail = userEntity.email,
                    usuarioId = usuarioUid,
                    displayName = finalDisplayName,
                    planUsuario = if (userEntity.esPremium) "PREMIUM" else "FREE",
                    isAuthenticated = true
                )

                if (userEntity.esPremium) {
                    _restoreAllowed.value = true; _restoreBlockMessage.value = null
                } else {
                    _restoreAllowed.value = false
                    _restoreBlockMessage.value = "Para recuperar tus datos en este dispositivo necesitas el plan Premium."
                }

                // Bootstrap/Sync según política de descarga
                handlePostLoginDataSync(usuarioUid, isPremium = userEntity.esPremium)
            } else {
                Log.w(TAG, "Config híbrida no disponible, aplicando fallback mínimo")
                ConfigurationManager.updateUserConfiguration(
                    usuarioId = usuarioUid,
                    usuarioEmail = Firebase.auth.currentUser?.email,
                    displayName = Firebase.auth.currentUser?.displayName,
                    isAuthenticated = true,
                    planUsuario = "FREE"
                )
                _restoreAllowed.value = false
                _restoreBlockMessage.value = "No se pudo cargar la configuración. Intenta de nuevo."
                _syncState.value = SyncState.Done
                setProgress(100, "Completado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error real cargando configuración híbrida", e)
            ConfigurationManager.updateUserConfiguration(
                usuarioId = usuarioUid,
                usuarioEmail = Firebase.auth.currentUser?.email,
                displayName = Firebase.auth.currentUser?.displayName,
                isAuthenticated = true,
                planUsuario = "FREE"
            )
            _restoreAllowed.value = false
            _restoreBlockMessage.value = "No se pudo cargar la configuración. Intenta de nuevo."
            _syncState.value = SyncState.Error(e.message ?: "Error de sincronización")
            setProgress(0, "Error")
        } finally {
            isLoadingConfiguration = false
        }
    }

    // ====== Núcleo bootstrap/sync ======
    private suspend fun handlePostLoginDataSync(uid: String, isPremium: Boolean) = withContext(Dispatchers.IO) {
        try {
            if (!isPremium) {
                Log.i(TAG, "FREE → no se restaura. (Paywall en UI)")
                _syncState.value = SyncState.Done
                setProgress(100, "Completado")
                return@withContext
            }

            val empty = isRoomEmptyForUser(uid)
            if (empty) {
                Log.i(TAG, "📦 Room VACÍO → Bootstrap TOTAL desde Firebase (Premium).")
                _syncState.value = SyncState.Downloading
                setProgress(5, "Iniciando descarga…")
                bootstrapFromFirebase(uid)
                _syncState.value = SyncState.Done
                setProgress(100, "Completado")
                return@withContext
            }

            // A partir de aquí: Room NO está vacío → NO descargar.
            val hasPendingLocal = hasLocalPendings(uid)
            if (hasPendingLocal) {
                Log.i(TAG, "🔁 Room NO vacío y HAY pendientes → SOLO subir pendientes. NO descargar nada.")
                _syncState.value = SyncState.Uploading
                setProgress(10, "Subiendo cambios locales…")
                pushLocalPendings(uid)
                _syncState.value = SyncState.Done
                setProgress(100, "Completado")
            } else {
                Log.i(TAG, "✅ Room NO vacío y SIN pendientes → NO se descarga nada de Firebase. Se respetan los datos locales.")
                _syncState.value = SyncState.Done
                setProgress(100, "Completado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en handlePostLoginDataSync", e)
            _syncState.value = SyncState.Error(e.message ?: "Error de sincronización")
            setProgress(0, "Error")
        }
    }

    // ---- Helpers de estado local ----
    private suspend fun isRoomEmptyForUser(uid: String): Boolean {
        val hasCats = categoriaDao.existeAlgunoDeUsuario(uid)
        val hasArts = articuloDao.existeAlgunoDeUsuario(uid)
        val hasMerc = mercadilloDao.existeAlgunoDeUsuario(uid)
        val hasRec  = recibosDao.existeAlgunoDeUsuario(uid)
        val hasLin  = lineasDao.existeAlgunoDeUsuario(uid)
        Log.d(TAG, "Estado Room → cats=$hasCats arts=$hasArts merc=$hasMerc rec=$hasRec lin=$hasLin")
        return !(hasCats || hasArts || hasMerc || hasRec || hasLin)
    }

    private suspend fun hasLocalPendings(uid: String): Boolean {
        val catsPend = categoriaDao.getCategoriasNoSincronizadasByUser(uid).isNotEmpty()
        val artsPend = articuloDao.getArticulosNoSincronizadosByUser(uid).isNotEmpty()
        val mercPend = mercadilloDao.getMercadillosNoSincronizadosByUser(uid).isNotEmpty()
        val result = catsPend || artsPend || mercPend
        Log.d(TAG, "Pendientes locales → cats=$catsPend arts=$artsPend merc=$mercPend -> $result")
        return result
    }

    // ---- Bootstrap TOTAL (Premium + Room vacío) ----
    private suspend fun bootstrapFromFirebase(uid: String) = withContext(Dispatchers.IO) {
        val remote = fetchAllFromFirebase(uid)
        setProgress(90, "Guardando en la base de datos…")
        db.withTransaction {
            categoriaDao.upsertAll(remote.categorias)
            articuloDao.upsertAll(remote.articulos)
            mercadilloDao.upsertAll(remote.mercadillos)
            recibosDao.upsertAll(remote.recibos)
            lineasDao.upsertAll(remote.lineas)

            remote.categorias.forEach { categoriaDao.marcarComoSincronizada(it.idCategoria) }
            remote.articulos.forEach  { articuloDao.marcarComoSincronizado(it.idArticulo) }
            remote.mercadillos.forEach{ mercadilloDao.marcarComoSincronizado(it.idMercadillo) }
        }
        Log.i(TAG, "✅ Bootstrap completado para uid=$uid -> cats=${remote.categorias.size}, arts=${remote.articulos.size}, merc=${remote.mercadillos.size}, rec=${remote.recibos.size}, lin=${remote.lineas.size}")
    }

    // ---- Reemplazo TOTAL (Room no vacío, sin pendientes) ----
    // (Se mantiene por compatibilidad, PERO ya NO se invoca en la política actual)
    private suspend fun replaceAllFromFirebase(uid: String) = withContext(Dispatchers.IO) {
        val remote = fetchAllFromFirebase(uid)
        setProgress(90, "Guardando en la base de datos…")
        db.withTransaction {
            categoriaDao.borrarPorUsuario(uid)
            articuloDao.borrarPorUsuario(uid)
            mercadilloDao.borrarPorUsuario(uid)
            recibosDao.borrarPorUsuario(uid)
            lineasDao.borrarPorUsuario(uid)

            categoriaDao.upsertAll(remote.categorias)
            articuloDao.upsertAll(remote.articulos)
            mercadilloDao.upsertAll(remote.mercadillos)
            recibosDao.upsertAll(remote.recibos)
            lineasDao.upsertAll(remote.lineas)

            remote.categorias.forEach { categoriaDao.marcarComoSincronizada(it.idCategoria) }
            remote.articulos.forEach  { articuloDao.marcarComoSincronizado(it.idArticulo) }
            remote.mercadillos.forEach{ mercadilloDao.marcarComoSincronizado(it.idMercadillo) }
        }
        Log.i(TAG, "✅ Reemplazo completo desde Firebase para uid=$uid")
    }

    // ---- Merge (subir pendientes y luego bajar actualizaciones) ----
    // (Se mantiene por compatibilidad, PERO ya NO se invoca en la política actual)
    private suspend fun pullAllFromFirebaseMerge(uid: String) = withContext(Dispatchers.IO) {
        val remote = fetchAllFromFirebase(uid)
        setProgress(90, "Guardando en la base de datos…")
        db.withTransaction {
            categoriaDao.upsertAll(remote.categorias)
            articuloDao.upsertAll(remote.articulos)
            mercadilloDao.upsertAll(remote.mercadillos)
            recibosDao.upsertAll(remote.recibos)
            lineasDao.upsertAll(remote.lineas)

            remote.categorias.forEach { categoriaDao.marcarComoSincronizada(it.idCategoria) }
            remote.articulos.forEach  { articuloDao.marcarComoSincronizado(it.idArticulo) }
            remote.mercadillos.forEach{ mercadilloDao.marcarComoSincronizado(it.idMercadillo) }
        }
        Log.i(TAG, "✅ Pull MERGE desde Firebase para uid=$uid")
    }

    private suspend fun pushLocalPendings(uid: String) {
        val cats = categoriaDao.getCategoriasNoSincronizadasByUser(uid)
        cats.forEach { c ->
            colCategorias(firestore, uid).document(c.idCategoria).set(c.toMap(), SetOptions.merge()).await()
            categoriaDao.marcarComoSincronizada(c.idCategoria)
        }
        val arts = articuloDao.getArticulosNoSincronizadosByUser(uid)
        arts.forEach { a ->
            colArticulos(firestore, uid).document(a.idArticulo).set(a.toMap(), SetOptions.merge()).await()
            articuloDao.marcarComoSincronizado(a.idArticulo)
        }
        val mercs = mercadilloDao.getMercadillosNoSincronizadosByUser(uid)
        mercs.forEach { m ->
            colMercadillos(firestore, uid).document(m.idMercadillo).set(m.toMap(), SetOptions.merge()).await()
            mercadilloDao.marcarComoSincronizado(m.idMercadillo)
        }
        Log.i(TAG, "⬆️ Pendientes locales subidos a Firebase (uid=$uid)")
    }

    // ---- Descarga remota con PROGRESO ----
    private suspend fun fetchAllFromFirebase(uid: String): RemoteBundle = withContext(Dispatchers.IO) {
        Log.d(TAG, "📥 Descargando datos TOP-LEVEL para uid=$uid")

        setProgress(10, "Descargando categorías…")
        val cats = try {
            colCategorias(firestore, uid)
                .whereEqualTo("userId", uid)
                .get().await().documents.mapNotNull { it.toCategoria(uid) }
        } catch (e: Exception) { Log.e(TAG, "Error descargando categorías", e); emptyList() }

        setProgress(30, "Descargando artículos…")
        val arts = try {
            colArticulos(firestore, uid)
                .whereEqualTo("userId", uid)
                .get().await().documents.mapNotNull { it.toArticulo(uid) }
        } catch (e: Exception) { Log.e(TAG, "Error descargando artículos", e); emptyList() }

        setProgress(45, "Descargando mercadillos…")
        val mercs = try {
            colMercadillos(firestore, uid)
                .whereEqualTo("userId", uid)
                .get().await().documents.mapNotNull { it.toMercadillo(uid) }
        } catch (e: Exception) { Log.e(TAG, "Error descargando mercadillos", e); emptyList() }

        setProgress(60, "Descargando recibos…")
        val recs = try {
            colRecibos(firestore, uid)
                .whereEqualTo("idUsuario", uid)
                .get().await().documents.mapNotNull { it.toRecibo(uid) }
        } catch (e: Exception) { Log.e(TAG, "Error descargando recibos", e); emptyList() }

        setProgress(80, "Descargando líneas de venta…")
        val lineas = try {
            colLineasVenta(firestore, uid)
                .whereEqualTo("idUsuario", uid)
                .get().await().documents.mapNotNull { snap ->
                    val idRecibo = (snap.data?.get("idRecibo") as? String).orElseEmpty()
                    snap.toLinea(uid, idRecibo)
                }
        } catch (e: Exception) { Log.e(TAG, "Error descargando líneas de venta", e); emptyList() }

        Log.d(TAG, "📦 Remoto → cats=${cats.size}, arts=${arts.size}, merc=${mercs.size}, rec=${recs.size}, lin=${lineas.size}")
        RemoteBundle(cats, arts, mercs, recs, lineas)
    }

    // ====== Registro/Login/Google Auth ======
    suspend fun registerWithEmail(email: String, password: String): AuthResult = try {
        Log.d(TAG, "Iniciando registro para: $email")
        _authState.value = AuthState.Loading
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user
        if (user != null) {
            userRepository.getOrCreateUser(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "",
                photoUrl = user.photoUrl?.toString() ?: "",
                esPremium = false
            )
            ConfigurationManager.updateUserConfiguration(
                usuarioId = user.uid,
                usuarioEmail = user.email,
                displayName = user.displayName,
                isAuthenticated = true,
                planUsuario = "FREE"
            )
            repoScope.launch { loadUserConfigurationHybrid(user.uid) }
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success(user)
        } else {
            val error = "Error: Usuario nulo después del registro"
            Log.e(TAG, error); _authState.value = AuthState.Error(error); AuthResult.Error(error)
        }
    } catch (e: Exception) {
        val errorMessage = "Error en registro: ${e.message}"
        Log.e(TAG, errorMessage, e); _authState.value = AuthState.Error(errorMessage); AuthResult.Error(errorMessage)
    }

    suspend fun loginWithEmail(email: String, password: String): AuthResult = try {
        Log.d(TAG, "Iniciando login para: $email")
        _authState.value = AuthState.Loading
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user
        if (user != null) {
            userRepository.getOrCreateUser(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            ConfigurationManager.updateUserConfiguration(
                usuarioId = user.uid,
                usuarioEmail = user.email,
                displayName = user.displayName,
                isAuthenticated = true,
                planUsuario = "FREE"
            )
            repoScope.launch { loadUserConfigurationHybrid(user.uid) }
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success(user)
        } else {
            val error = "Error: Usuario nulo después del login"
            Log.e(TAG, error); _authState.value = AuthState.Error(error); AuthResult.Error(error)
        }
    } catch (e: Exception) {
        val errorMessage = when (e.message) {
            "The email address is badly formatted." -> "Email inválido"
            "The password is invalid or the user does not have a password." -> "Contraseña incorrecta"
            "There is no user record corresponding to this identifier. The user may have been deleted." -> "Usuario no encontrado"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Error de conexión"
            else -> "Error en login: ${e.message}"
        }
        Log.e(TAG, errorMessage, e); _authState.value = AuthState.Error(errorMessage); AuthResult.Error(errorMessage)
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult = try {
        Log.d(TAG, "Iniciando Google Auth con idToken")
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val user = authResult.user
        if (user != null) {
            userRepository.getOrCreateUser(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            ConfigurationManager.updateUserConfiguration(
                usuarioId = user.uid,
                usuarioEmail = user.email,
                displayName = user.displayName,
                isAuthenticated = true,
                planUsuario = "FREE"
            )
            repoScope.launch { loadUserConfigurationHybrid(user.uid) }
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success(user)
        } else {
            val error = "Error: Usuario nulo después de Google Auth"
            Log.e(TAG, error); _authState.value = AuthState.Error(error); AuthResult.Error(error)
        }
    } catch (e: Exception) {
        val errorMessage = when (e.message) {
            "An internal error has occurred. [ INVALID_IDP_RESPONSE ]" -> "Error de autenticación con Google"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Error de conexión"
            else -> "Error en Google Auth: ${e.message}"
        }
        Log.e(TAG, errorMessage, e); _authState.value = AuthState.Error(errorMessage); AuthResult.Error(errorMessage)
    }

    suspend fun logout(): AuthResult = try {
        Log.d(TAG, "Cerrando sesión...")
        _authState.value = AuthState.Loading
        firebaseAuth.signOut()
        configuracionRepository.setUsuarioLogueado("usuario_default")
        ConfigurationManager.updateUserConfiguration(
            idioma = "es",
            fuente = "Montserrat",
            temaOscuro = false,
            moneda = "€ Euro",
            usuarioEmail = null,
            usuarioId = null,
            displayName = null,
            isAuthenticated = false,
            planUsuario = "FREE"
        )
        _restoreAllowed.value = null
        _restoreBlockMessage.value = null
        _syncState.value = SyncState.Idle
        setProgress(0, "")
        Log.d(TAG, "✅ Sesión cerrada y configuración reseteada")
        _authState.value = AuthState.Unauthenticated
        AuthResult.Success(null)
    } catch (e: Exception) {
        val errorMessage = "Error al cerrar sesión: ${e.message}"
        Log.e(TAG, errorMessage, e); _authState.value = AuthState.Error(errorMessage); AuthResult.Error(errorMessage)
    }

    // ====== API legacy/compat ======
    suspend fun updateUserProfile(userId: String, displayName: String, email: String) {
        try { userRepository.updateUserProfile(userId, displayName, email) } catch (e: Exception) { Log.e(TAG,"❌",e) }
    }
    suspend fun updateUserProfileAndMarkDirty(userId: String, displayName: String, email: String) {
        try { userRepository.updateUserProfileAndMarkDirty(userId, displayName, email) } catch (e: Exception) { Log.e(TAG,"❌",e) }
    }
    suspend fun updateUserInDatabase(userId: String, displayName: String, email: String) {
        try { userRepository.updateUserProfile(userId, displayName, email) } catch (e: Exception) { Log.e(TAG,"❌",e) }
    }
    suspend fun loadUserConfiguration(usuarioUid: String, esPremium: Boolean) {
        try {
            val configEntity = configuracionRepository.getConfiguracion()
            configuracionRepository.setUsuarioLogueado(usuarioUid)
            val userEntity = userRepository.getUserById(usuarioUid)
            if (configEntity != null && userEntity != null) {
                ConfigurationManager.updateUserConfiguration(
                    idioma = configEntity.idioma,
                    fuente = configEntity.fuente,
                    temaOscuro = configEntity.temaOscuro,
                    moneda = configEntity.moneda,
                    usuarioEmail = userEntity.email,
                    usuarioId = usuarioUid,
                    displayName = userEntity.displayName,
                    planUsuario = if (userEntity.esPremium) "PREMIUM" else "FREE",
                    isAuthenticated = true
                )
            } else {
                ConfigurationManager.updateUserConfiguration(
                    usuarioId = usuarioUid,
                    usuarioEmail = Firebase.auth.currentUser?.email,
                    displayName = Firebase.auth.currentUser?.displayName,
                    isAuthenticated = true,
                    planUsuario = if (esPremium) "PREMIUM" else "FREE"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando configuración (legacy)", e)
        }
    }
    suspend fun updateConfiguration(
        idioma: String?=null, fuente: String?=null, temaOscuro: Boolean?=null, moneda: String?=null
    ): Boolean = try {
        val canChangeAdvanced = ConfigurationManager.canChangeConfiguration()
        if (idioma != null && canChangeAdvanced) { configuracionRepository.updateIdioma(idioma); ConfigurationManager.setIdioma(idioma) }
        if (fuente != null && canChangeAdvanced) { configuracionRepository.updateFuente(fuente); ConfigurationManager.setFuente(fuente) }
        if (moneda != null && canChangeAdvanced) { configuracionRepository.updateMoneda(moneda); ConfigurationManager.setMoneda(moneda) }
        if (temaOscuro != null) { configuracionRepository.updateTemaOscuro(temaOscuro); ConfigurationManager.setTemaOscuro(temaOscuro) }
        true
    } catch (e: Exception) { Log.e(TAG,"Error actualizando configuración",e); false }

    suspend fun updateUserPremium(esPremium: Boolean): Boolean = try {
        val currentUser = firebaseAuth.currentUser ?: return false
        userRepository.updateUserPremium(currentUser.uid, esPremium)
        ConfigurationManager.updateUserPremium(esPremium)
        if (esPremium) { _restoreAllowed.value = true; _restoreBlockMessage.value = null }
        else { _restoreAllowed.value = false; _restoreBlockMessage.value = "Para recuperar tus datos en este dispositivo necesitas el plan Premium." }
        Log.d(TAG, "✅ Estado Premium actualizado: $esPremium"); true
    } catch (e: Exception) { Log.e(TAG, "Error actualizando estado Premium", e); false }

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

// ====== Soportes internos ======
private data class RemoteBundle(
    val categorias: List<CategoriaEntity>,
    val articulos: List<ArticuloEntity>,
    val mercadillos: List<MercadilloEntity>,
    val recibos: List<ReciboEntity>,
    val lineas: List<LineaVentaEntity>
)

private fun CategoriaEntity.toMap(): Map<String, Any?> = mapOf(
    "idCategoria" to idCategoria,
    "userId" to userId,
    "nombre" to nombre,
    "colorHex" to colorHex,
    "orden" to orden,
    "activa" to activa,
    "version" to version,
    "lastModified" to lastModified,
    "sincronizadoFirebase" to true
)

private fun ArticuloEntity.toMap(): Map<String, Any?> = mapOf(
    "idArticulo" to idArticulo,
    "userId" to userId,
    "nombre" to nombre,
    "idCategoria" to idCategoria,
    "precioVenta" to precioVenta,
    "precioCoste" to precioCoste,
    "stock" to stock,
    "controlarStock" to controlarStock,
    "controlarCoste" to controlarCoste,
    "favorito" to favorito,
    "fotoUri" to fotoUri,
    "activo" to activo,
    "version" to version,
    "lastModified" to lastModified,
    "sincronizadoFirebase" to true
)

private fun MercadilloEntity.toMap(): Map<String, Any?> = mapOf(
    "idMercadillo" to idMercadillo,
    "userId" to userId,
    "fecha" to fecha,
    "lugar" to lugar,
    "organizador" to organizador,
    "esGratis" to esGratis,
    "importeSuscripcion" to importeSuscripcion,
    "requiereMesa" to requiereMesa,
    "requiereCarpa" to requiereCarpa,
    "hayPuntoLuz" to hayPuntoLuz,
    "horaInicio" to horaInicio,
    "horaFin" to horaFin,
    "estado" to estado,
    "pendienteArqueo" to pendienteArqueo,
    "pendienteAsignarSaldo" to pendienteAsignarSaldo,
    "saldoInicial" to saldoInicial,
    "saldoFinal" to saldoFinal,
    "arqueoCaja" to arqueoCaja,
    "totalVentas" to totalVentas,
    "totalGastos" to totalGastos,
    "arqueoMercadillo" to arqueoMercadillo,
    "activo" to activo,
    "version" to version,
    "lastModified" to lastModified,
    "sincronizadoFirebase" to true
)

private fun Map<String, Any?>.getString(key: String, def: String = ""): String =
    (this[key] as? String) ?: def
private fun Map<String, Any?>.getDouble(key: String, def: Double = 0.0): Double =
    when (val v = this[key]) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: def; else -> def }
private fun Map<String, Any?>.getInt(key: String, def: Int = 0): Int =
    when (val v = this[key]) { is Number -> v.toInt(); is String -> v.toIntOrNull() ?: def; else -> def }
private fun Map<String, Any?>.getBool(key: String, def: Boolean = false): Boolean =
    when (val v = this[key]) { is Boolean -> v; is Number -> v.toInt()!=0; is String -> v=="true"; else -> def }
private fun Map<String, Any?>.getLong(key: String, def: Long = System.currentTimeMillis()): Long =
    when (val v = this[key]) { is Number -> v.toLong(); is String -> v.toLongOrNull() ?: def; else -> def }

// 🔐 Helper para verificar que el documento remoto pertenece al UID actual
private fun Map<String, Any?>.getUidField(): String? =
    (this["userId"] as? String) ?: (this["idUsuario"] as? String)
private fun String?.orElseEmpty(): String = this ?: ""

// ==== Mappers con guardia de UID (defensa en profundidad) ====

private fun com.google.firebase.firestore.DocumentSnapshot.toCategoria(uid: String): CategoriaEntity? {
    val m = this.data ?: return null
    val remoteUid = m.getUidField()
    if (remoteUid != null && remoteUid != uid) return null

    return CategoriaEntity(
        idCategoria = m.getString("idCategoria", this.id),
        userId = uid,
        nombre = m.getString("nombre"),
        colorHex = m.getString("colorHex", "#FFFFFF"),
        orden = m.getInt("orden", 0),
        activa = m.getBool("activa", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toArticulo(uid: String): ArticuloEntity? {
    val m = this.data ?: return null
    val remoteUid = m.getUidField()
    if (remoteUid != null && remoteUid != uid) return null

    return ArticuloEntity(
        idArticulo = m.getString("idArticulo", this.id),
        userId = uid,
        nombre = m.getString("nombre"),
        idCategoria = m.getString("idCategoria"),
        precioVenta = m.getDouble("precioVenta", 0.0),
        precioCoste = (m["precioCoste"] as? Number)?.toDouble(),
        stock = (m["stock"] as? Number)?.toInt(),
        controlarStock = m.getBool("controlarStock", false),
        controlarCoste = m.getBool("controlarCoste", false),
        favorito = m.getBool("favorito", false),
        fotoUri = m["fotoUri"] as? String,
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toMercadillo(uid: String): MercadilloEntity? {
    val m = this.data ?: return null
    val remoteUid = m.getUidField()
    if (remoteUid != null && remoteUid != uid) return null

    return MercadilloEntity(
        idMercadillo = m.getString("idMercadillo", this.id),
        userId = uid,
        fecha = m.getString("fecha"),
        lugar = m.getString("lugar"),
        organizador = m.getString("organizador"),
        esGratis = m.getBool("esGratis", true),
        importeSuscripcion = m.getDouble("importeSuscripcion", 0.0),
        requiereMesa = m.getBool("requiereMesa", true),
        requiereCarpa = m.getBool("requiereCarpa", true),
        hayPuntoLuz = m.getBool("hayPuntoLuz", false),
        horaInicio = m.getString("horaInicio", "09:00"),
        horaFin = m.getString("horaFin", "14:00"),
        estado = m.getInt("estado", 1),
        pendienteArqueo = m.getBool("pendienteArqueo", false),
        pendienteAsignarSaldo = m.getBool("pendienteAsignarSaldo", false),
        saldoInicial = (m["saldoInicial"] as? Number)?.toDouble(),
        saldoFinal = (m["saldoFinal"] as? Number)?.toDouble(),
        arqueoCaja = (m["arqueoCaja"] as? Number)?.toDouble(),
        totalVentas = m.getDouble("totalVentas", 0.0),
        totalGastos = m.getDouble("totalGastos", 0.0),
        arqueoMercadillo = (m["arqueoMercadillo"] as? Number)?.toDouble(),
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toRecibo(uid: String): ReciboEntity? {
    val m = this.data ?: return null
    val remoteUid = m.getUidField()
    if (remoteUid != null && remoteUid != uid) return null

    return ReciboEntity(
        idRecibo = m.getString("idRecibo", this.id),
        idMercadillo = m.getString("idMercadillo"),
        idUsuario = uid,
        fechaHora = m.getLong("fechaHora", System.currentTimeMillis()),
        metodoPago = m.getString("metodoPago", "efectivo"),
        totalTicket = m.getDouble("totalTicket", 0.0),
        estado = m.getString("estado", "COMPLETADO")
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toLinea(uid: String, idRecibo: String): LineaVentaEntity? {
    val m = this.data ?: return null
    val remoteUid = m.getUidField()
    if (remoteUid != null && remoteUid != uid) return null

    return LineaVentaEntity(
        idLinea = m.getString("idLinea", this.id),
        idRecibo = idRecibo,
        idMercadillo = m.getString("idMercadillo"),
        idUsuario = uid,
        numeroLinea = m.getInt("numeroLinea", 1),
        tipoLinea = m.getString("tipoLinea", "producto"),
        descripcion = m.getString("descripcion"),
        idProducto = m["idProducto"] as? String,
        cantidad = m.getInt("cantidad", 1),
        precioUnitario = m.getDouble("precioUnitario", 0.0),
        subtotal = m.getDouble("subtotal", 0.0),
        idLineaOriginalAbonada = m["idLineaOriginalAbonada"] as? String
    )
}

