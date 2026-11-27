package es.nuskysoftware.marketsales.data.repository
import es.nuskysoftware.marketsales.utils.DemoAccess


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
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*
import es.nuskysoftware.marketsales.data.local.dao.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

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
    val context: Context
) {
    private fun text(key: String) = StringResourceManager.getString(key, ConfigurationManager.idioma.value)

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val appContext = context.applicationContext
    private val firestore = FirebaseFirestore.getInstance()

    private val _restoreAllowed = MutableStateFlow<Boolean?>(null)
    val restoreAllowed: StateFlow<Boolean?> = _restoreAllowed
    private val _restoreBlockMessage = MutableStateFlow<String?>(null)
    val restoreBlockMessage: StateFlow<String?> = _restoreBlockMessage

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
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

    private val userRepository = UserRepository(context)
    private val configuracionRepository = ConfiguracionRepository(context)

    private val db: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    private val categoriaDao: CategoriaDao by lazy { db.categoriaDao() }
    private val articuloDao: ArticuloDao by lazy { db.articuloDao() }
    private val mercadilloDao: MercadilloDao by lazy { db.mercadilloDao() }
    private val recibosDao: RecibosDao by lazy { db.recibosDao() }
    private val lineasDao: LineasVentaDao by lazy { db.lineasVentaDao() }
    private val lineasGastosDao: LineasGastosDao by lazy { db.lineasGastosDao() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AuthRepository"

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

        private fun colLineasGastos(db: FirebaseFirestore, @Suppress("UNUSED_PARAMETER") uid: String) =
            db.collection("LineasGastos")
    }

    init {
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        _authState.value = if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated

        if (user != null) {
            if (ConfigurationManager.getCurrentUserId() != user.uid) ConfigurationManager.login(user.uid, user.email, user.displayName)
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
                        planUsuario = "FREE",
                    )
                }
            }
        } else {
            ConfigurationManager.logout()
            Log.d(TAG, "No hay usuario - ConfigurationManager en estado demo")
        }

        firebaseAuth.addAuthStateListener { auth ->
            val current = auth.currentUser
            if (current == null) ConfigurationManager.logout()
            if (current != null) {
                runCatching { es.nuskysoftware.marketsales.work.StockMovementScheduler.start(context.applicationContext) }
                    .onFailure { Log.w(TAG, "Unable to schedule stock retry", it) }
            }
            _currentUser.value = current
            if (current == null || _authState.value !is AuthState.Loading) {
                _authState.value = if (current != null) AuthState.Authenticated(current) else AuthState.Unauthenticated
            }
        }

        Log.d(TAG, "AuthRepository inicializado - Usuario actual: ${user?.email ?: "null"}")
    }

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

    private suspend fun loadUserConfigurationHybrid(usuarioUid: String) {
        if (!DemoAccess.canSync(usuarioUid)) return
        if (isLoadingConfiguration) {
            Log.d(TAG, "Ya se está cargando configuración, omitiendo")
            return
        }

        isLoadingConfiguration = true
        _syncState.value = SyncState.Checking
        setProgress(0, text("sync_preparando"))

        try {
            Log.d(TAG, "🔄 Cargando configuración HÍBRIDA para usuario: $usuarioUid")

            val configEntity = configuracionRepository.getConfiguracion()
            configuracionRepository.setUsuarioLogueado(usuarioUid)
            val userEntity = userRepository.getUserById(usuarioUid)
            if (!DemoAccess.canSync(usuarioUid)) return

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

                if (userEntity.esPremium) {
                    _restoreAllowed.value = true
                    _restoreBlockMessage.value = null
                } else {
                    _restoreAllowed.value = false
                    _restoreBlockMessage.value =
                        text("restauracion_requiere_premium")
                }

                handlePostLoginDataSync(usuarioUid, isPremium = userEntity.esPremium)
            } else {
                Log.w(TAG, "Config híbrida no disponible, aplicando fallback mínimo")
                ConfigurationManager.updateUserConfiguration(
                    usuarioId = usuarioUid,
                    usuarioEmail = Firebase.auth.currentUser?.email,
                    displayName = Firebase.auth.currentUser?.displayName,
                    isAuthenticated = true,
                    planUsuario = "FREE",
                )
                _restoreAllowed.value = false
                _restoreBlockMessage.value = text("configuracion_no_cargada")
                _syncState.value = SyncState.Done
                setProgress(100, text("sync_completado"))
            }
        } catch (e: Exception) {
            if (!DemoAccess.canSync(usuarioUid)) return
            Log.e(TAG, "Error real cargando configuración híbrida", e)
            ConfigurationManager.updateUserConfiguration(
                usuarioId = usuarioUid,
                usuarioEmail = Firebase.auth.currentUser?.email,
                displayName = Firebase.auth.currentUser?.displayName,
                isAuthenticated = true,
                planUsuario = "FREE",
            )
            _restoreAllowed.value = false
            _restoreBlockMessage.value = text("configuracion_no_cargada")
            _syncState.value = SyncState.Error(e.message ?: text("error_en_sincronizacion"))
            setProgress(0, "Error")
        } finally {
            isLoadingConfiguration = false
        }
    }

    suspend fun handlePostLoginDataSync(uid: String, isPremium: Boolean) = withContext(Dispatchers.IO) {
        if (!DemoAccess.canSync(uid)) return@withContext
        try {
            SyncEngine(appContext).sync(uid)
            _syncState.value = SyncState.Done
            setProgress(100, text("sync_completado"))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (failure: Exception) {
            // A network failure is not a failed local write or a reason to end the session.
            es.nuskysoftware.marketsales.work.BusinessSyncScheduler.request(appContext)
            _syncState.value = SyncState.Done
        }
    }

    private suspend fun isRoomEmptyForUser(uid: String): Boolean {
        val hasCats = categoriaDao.existeAlgunoDeUsuario(uid)
        val hasArts = articuloDao.existeAlgunoDeUsuario(uid)
        val hasMerc = mercadilloDao.existeAlgunoDeUsuario(uid)
        val hasRec = recibosDao.existeAlgunoDeUsuario(uid)
        val hasLin = lineasDao.existeAlgunoDeUsuario(uid)
        val hasGas = lineasGastosDao.existeAlgunoDeUsuario(uid)
        Log.d(
            TAG,
            "Estado Room → cats=$hasCats arts=$hasArts merc=$hasMerc rec=$hasRec lin=$hasLin gas=$hasGas"
        )
        return !(hasCats || hasArts || hasMerc || hasRec || hasLin || hasGas)
    }

    private suspend fun hasLocalPendings(uid: String): Boolean {
        val catsPend = categoriaDao.getCategoriasNoSincronizadasByUser(uid).isNotEmpty()
        val artsPend = articuloDao.getArticulosNoSincronizadosByUser(uid).isNotEmpty()
        val mercPend = mercadilloDao.getMercadillosNoSincronizadosByUser(uid).isNotEmpty()
        val recPend = recibosDao.getPendientesSync(uid).isNotEmpty()
        val lineasPend = lineasDao.getPendientesSync(uid).isNotEmpty()
        val gastosPend = lineasGastosDao.getPendientesSync(uid).isNotEmpty()

        val result = catsPend || artsPend || mercPend || recPend || lineasPend || gastosPend
        Log.d(
            TAG,
            "Pendientes locales → cats=$catsPend arts=$artsPend merc=$mercPend rec=$recPend lin=$lineasPend gas=$gastosPend -> $result"
        )
        return result
    }

    private suspend fun bootstrapFromFirebase(uid: String) = withContext(Dispatchers.IO) {
        SyncEngine(appContext).sync(uid)
    }

    private suspend fun replaceAllFromFirebase(uid: String) = withContext(Dispatchers.IO) {
        SyncEngine(appContext).sync(uid)
    }

    private suspend fun pullAllFromFirebaseMerge(uid: String) = withContext(Dispatchers.IO) {
        SyncEngine(appContext).sync(uid)
    }

    private suspend fun pushLocalPendings(uid: String) {
        SyncEngine(appContext).sync(uid)
    }

    private suspend fun fetchAllFromFirebase(uid: String): RemoteBundle = withContext(Dispatchers.IO) {
        Log.d(TAG, "📥 Descargando datos TOP-LEVEL para uid=$uid")

        setProgress(10, text("sync_categorias"))
        val cats = try {
            colCategorias(firestore, uid)
                .whereEqualTo("userId", uid)
                .whereEqualTo("activa", true)
                .get().await().documents.mapNotNull { it.toCategoria(uid) }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando categorías", e); emptyList()
        }

        setProgress(30, text("sync_articulos"))
        val arts = try {
            colArticulos(firestore, uid)
                .whereEqualTo("userId", uid)
                .whereEqualTo("activo", true)
                .get().await().documents.mapNotNull { it.toArticulo(uid) }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando artículos", e); emptyList()
        }

        setProgress(45, text("sync_mercadillos"))
        val mercs = try {
            colMercadillos(firestore, uid)
                .whereEqualTo("userId", uid)
                .whereEqualTo("activo", true)
                .get().await().documents.mapNotNull { it.toMercadillo(uid) }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando mercadillos", e); emptyList()
        }

        setProgress(60, text("sync_recibos"))
        val recs = try {
            colRecibos(firestore, uid)
                .whereEqualTo("idUsuario", uid)
                .whereEqualTo("activo", true)
                .get().await().documents.mapNotNull { it.toRecibo(uid) }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando recibos", e); emptyList()
        }

        setProgress(70, text("sync_gastos"))
        val gastos = try {
            colLineasGastos(firestore, uid)
                .whereEqualTo("idUsuario", uid)
                .whereEqualTo("activo", true)
                .get().await().documents.mapNotNull { snap ->
                    snap.toLineaGasto(uid)?.let { gasto -> snap.id to gasto }
                }.groupBy { (_, gasto) -> gasto.idMercadillo to gasto.numeroLinea }
                .values.map { copies ->
                    // Un documento antiguo incompleto no debe sustituir al canónico.
                    copies.maxWithOrNull(compareBy<Pair<String, LineaGastoEntity>>(
                        { (id, gasto) -> id == GastoSync.documentId(gasto) },
                        { (_, gasto) -> gasto.lastModified }
                    ))!!.second
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando gastos", e); emptyList()
        }

        setProgress(80, text("sync_lineas"))
        val lineas = try {
            colLineasVenta(firestore, uid)
                .whereEqualTo("idUsuario", uid)
                .whereEqualTo("activo", true)
                .get().await().documents.mapNotNull { snap ->
                    val idRecibo = (snap.data?.get("idRecibo") as? String).orElseEmpty()
                    snap.toLinea(uid, idRecibo)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando líneas de venta", e); emptyList()
        }

        Log.d(
            TAG,
            "📦 Remoto → cats=${cats.size}, arts=${arts.size}, merc=${mercs.size}, rec=${recs.size}, gas=${gastos.size}, lin=${lineas.size}"
        )
        RemoteBundle(cats, arts, mercs, recs, gastos, lineas)
    }

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
                planUsuario = "FREE",
            )
            repoScope.launch { loadUserConfigurationHybrid(user.uid) }
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success(user)
        } else {
            val error = text("auth_usuario_nulo_registro")
            Log.e(TAG, error)
            _authState.value = AuthState.Error(error)
            AuthResult.Error(error)
        }
    } catch (e: Exception) {
        val errorMessage = text("auth_error_registro").replace("{0}", e.message.orEmpty())
        Log.e(TAG, errorMessage, e)
        _authState.value = AuthState.Error(errorMessage)
        AuthResult.Error(errorMessage)
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
                photoUrl = user.photoUrl?.toString() ?: "",
            )
            ConfigurationManager.updateUserConfiguration(
                usuarioId = user.uid,
                usuarioEmail = user.email,
                displayName = user.displayName,
                isAuthenticated = true,
                planUsuario = "FREE",
            )
            repoScope.launch { loadUserConfigurationHybrid(user.uid) }
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success(user)
        } else {
            val error = text("auth_usuario_nulo_login")
            Log.e(TAG, error)
            _authState.value = AuthState.Error(error)
            AuthResult.Error(error)
        }
    } catch (e: Exception) {
        val errorMessage = when (e.message) {
            "The email address is badly formatted." -> text("email_invalido")
            "The password is invalid or the user does not have a password." -> text("auth_password_incorrecta")
            "There is no user record corresponding to this identifier. The user may have been deleted." -> text("auth_usuario_no_encontrado")
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> text("auth_error_conexion")
            else -> text("auth_error_login").replace("{0}", e.message.orEmpty())
        }
        Log.e(TAG, errorMessage, e)
        _authState.value = AuthState.Error(errorMessage)
        AuthResult.Error(errorMessage)
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
                photoUrl = user.photoUrl?.toString() ?: "",
            )
            ConfigurationManager.updateUserConfiguration(
                usuarioId = user.uid,
                usuarioEmail = user.email,
                displayName = user.displayName,
                isAuthenticated = true,
                planUsuario = "FREE",
            )
            repoScope.launch { loadUserConfigurationHybrid(user.uid) }
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success(user)
        } else {
            val error = text("auth_usuario_nulo_google")
            Log.e(TAG, error)
            _authState.value = AuthState.Error(error)
            AuthResult.Error(error)
        }
    } catch (e: Exception) {
        val errorMessage = when (e.message) {
            "An internal error has occurred. [ INVALID_IDP_RESPONSE ]" -> text("auth_google_invalido")
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> text("auth_error_conexion")
            else -> text("auth_error_google").replace("{0}", e.message.orEmpty())
        }
        Log.e(TAG, errorMessage, e)
        _authState.value = AuthState.Error(errorMessage)
        AuthResult.Error(errorMessage)
    }

    suspend fun logout(): AuthResult = try {
        Log.d(TAG, "Cerrando sesión...")
        _authState.value = AuthState.Loading
        firebaseAuth.signOut()
        configuracionRepository.setUsuarioLogueado(es.nuskysoftware.marketsales.utils.AppModePolicy.DEMO_USER_ID)
        ConfigurationManager.logout()
        _restoreAllowed.value = null
        _restoreBlockMessage.value = null
        _syncState.value = SyncState.Idle
        setProgress(0, "")
        Log.d(TAG, "✅ Sesión cerrada y configuración reseteada")
        _authState.value = AuthState.Unauthenticated
        AuthResult.Success(null)
    } catch (e: Exception) {
        val errorMessage = text("auth_error_logout").replace("{0}", e.message.orEmpty())
        Log.e(TAG, errorMessage, e)
        _authState.value = AuthState.Error(errorMessage)
        AuthResult.Error(errorMessage)
    }

    suspend fun updateUserProfile(userId: String, displayName: String, email: String) {
        DemoAccess.requireOwner(userId)
        userRepository.updateUserProfile(userId, displayName, email)
    }

    suspend fun updateUserProfileAndMarkDirty(userId: String, displayName: String, email: String) {
        DemoAccess.requireOwner(userId)
        userRepository.updateUserProfileAndMarkDirty(userId, displayName, email)
    }

    suspend fun updateUserInDatabase(userId: String, displayName: String, email: String) {
        DemoAccess.requireOwner(userId)
        userRepository.updateUserProfile(userId, displayName, email)
    }

    suspend fun loadUserConfiguration(usuarioUid: String, esPremium: Boolean) {
        if (!DemoAccess.canSync(usuarioUid)) return
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
        idioma: String? = null,
        fuente: String? = null,
        temaOscuro: Boolean? = null,
        moneda: String? = null
    ): Boolean = try {
        val canChangeAdvanced = ConfigurationManager.canChangeConfiguration()
        if (idioma != null && canChangeAdvanced) {
            configuracionRepository.updateIdioma(idioma)
            ConfigurationManager.setIdioma(idioma)
        }
        if (fuente != null && canChangeAdvanced) {
            configuracionRepository.updateFuente(fuente)
            ConfigurationManager.setFuente(fuente)
        }
        if (moneda != null && canChangeAdvanced) {
            configuracionRepository.updateMoneda(moneda)
            ConfigurationManager.setMoneda(moneda)
        }
        if (temaOscuro != null) {
            configuracionRepository.updateTemaOscuro(temaOscuro)
            ConfigurationManager.setTemaOscuro(temaOscuro)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error actualizando configuración", e)
        false
    }

    suspend fun updateUserPremium(esPremium: Boolean): Boolean = try {
        val currentUser = firebaseAuth.currentUser ?: return false
        userRepository.updateUserPremium(currentUser.uid, esPremium)
        ConfigurationManager.updateUserPremium(esPremium)
        if (esPremium) {
            _restoreAllowed.value = true
            _restoreBlockMessage.value = null
        } else {
            _restoreAllowed.value = false
            _restoreBlockMessage.value =
                text("restauracion_requiere_premium")
        }
        Log.d(TAG, "✅ Estado Premium actualizado: $esPremium")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error actualizando estado Premium", e)
        false
    }

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

    suspend fun shouldShowDefaultUserWarning(): Boolean = false


    private suspend fun deactivateAllUserData(uid: String) {
        if (!DemoAccess.canSync(uid)) return
        val fs = FirebaseFirestore.getInstance()
        val collections = listOf(
            "categorias" to "activa",
            "articulos" to "activo",
            "mercadillos" to "activo",
            "recibos" to "activo",
            "lineas_venta" to "activo",
            "LineasGastos" to "activo"
        )
        for ((col, field) in collections) {
            val userField = if (col == "recibos" || col == "lineas_venta" || col == "LineasGastos") "idUsuario" else "userId"
            val snap = fs.collection(col)
                .whereEqualTo(userField, uid)
                .whereEqualTo(field, true)
                .get().await()
            if (snap.isEmpty) continue
            val batch = fs.batch()
            snap.documents.forEach { doc -> batch.update(doc.reference, field, false) }
            batch.commit().await()
        }
    }
}

private data class RemoteBundle(
    val categorias: List<CategoriaEntity>,
    val articulos: List<ArticuloEntity>,
    val mercadillos: List<MercadilloEntity>,
    val recibos: List<ReciboEntity>,
    val gastos: List<LineaGastoEntity>,
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
    when (val v = this[key]) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: def
        else -> def
    }

private fun Map<String, Any?>.getInt(key: String, def: Int = 0): Int =
    when (val v = this[key]) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: def
        else -> def
    }

private fun Map<String, Any?>.getBool(key: String, def: Boolean = false): Boolean =
    when (val v = this[key]) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v == "true"
        else -> def
    }

private fun Map<String, Any?>.getLong(key: String, def: Long = System.currentTimeMillis()): Long =
    when (val v = this[key]) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull() ?: def
        else -> def
    }

private fun Map<String, Any?>.getUidField(): String? =
    (this["userId"] as? String) ?: (this["idUsuario"] as? String)

private fun String?.orElseEmpty(): String = this ?: ""

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
        estado = m.getString("estado", "COMPLETADO"),
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
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
        idLineaOriginalAbonada = m["idLineaOriginalAbonada"] as? String,
        activo = m.getBool("activo", true),
        version = m.getLong("version", 1),
        lastModified = m.getLong("lastModified", System.currentTimeMillis()),
        sincronizadoFirebase = true
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toLineaGasto(uid: String): LineaGastoEntity? {
    val m = this.data ?: return null
    val remoteUid = (m["userId"] as? String) ?: (m["idUsuario"] as? String)
    if (remoteUid != null && remoteUid != uid) return null

    val mercadillo = (m["idMercadillo"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val numero = (m["numeroLinea"] as? String)?.takeIf { it.isNotBlank() }
        ?: (m["idLineaGasto"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    return LineaGastoEntity(
        idMercadillo = mercadillo,
        idUsuario = uid,
        numeroLinea = numero,
        descripcion = m["descripcion"] as? String ?: "",
        importe = (m["importe"] as? Number)?.toDouble() ?: 0.0,
        fechaHora = (m["fechaHora"] as? Number)?.toLong() ?: (m["lastModified"] as? Number)?.toLong() ?: 0L,
        formaPago = m["formaPago"] as? String ?: "efectivo",
        descontarDeCaja = (m["descontarDeCaja"] as? Boolean) ?: true,
        version = (m["version"] as? Number)?.toLong() ?: 1L,
        lastModified = (m["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        sincronizadoFirebase = true,
        activo = (m["activo"] as? Boolean) ?: true
    )
}




//package es.nuskysoftware.marketsales.data.repository
//
