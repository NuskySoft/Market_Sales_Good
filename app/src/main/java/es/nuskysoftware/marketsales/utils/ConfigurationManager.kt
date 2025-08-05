// app/src/main/java/es/nuskysoftware/marketsales/utils/ConfigurationManager.kt
package es.nuskysoftware.marketsales.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton global para gestionar configuración dinámica sin @Composable
 * ✅ AMPLIADO: Incluye sistema de usuarios y permisos para FREE/PREMIUM
 */
object ConfigurationManager {

    // ========== ESTADOS EXISTENTES (preservados) ==========
    private val _idioma = MutableStateFlow("es")
    private val _fuente = MutableStateFlow("Montserrat")
    private val _temaOscuro = MutableStateFlow(false)
    private val _versionApp = MutableStateFlow(0) // 0=FREE, 1=PREMIUM
    private val _usuarioEmail = MutableStateFlow<String?>(null)
    private val _usuarioId = MutableStateFlow<String?>(null)

    // ========== NUEVOS ESTADOS DE USUARIO (Sistema de Permisos) ==========
    private val _planUsuario = MutableStateFlow("FREE") // FREE, PREMIUM
    private val _empresaId = MutableStateFlow<String?>(null) // NULL para FREE, empresaId para PREMIUM
    private val _tipoUsuario = MutableStateFlow<String?>(null) // NULL para FREE, SUPER_ADMIN/ADMIN/EMPLEADO/INVITADO para PREMIUM
    private val _displayName = MutableStateFlow<String?>(null)
    private val _photoUrl = MutableStateFlow<String?>(null)
    private val _isAuthenticated = MutableStateFlow(false)

    // Estados públicos de solo lectura (existentes)
    val idioma: StateFlow<String> = _idioma.asStateFlow()
    val fuente: StateFlow<String> = _fuente.asStateFlow()
    val temaOscuro: StateFlow<Boolean> = _temaOscuro.asStateFlow()
    val versionApp: StateFlow<Int> = _versionApp.asStateFlow()
    val usuarioEmail: StateFlow<String?> = _usuarioEmail.asStateFlow()
    val usuarioId: StateFlow<String?> = _usuarioId.asStateFlow()

    // ✅ NUEVOS Estados públicos de usuario (Sistema de Permisos)
    val planUsuario: StateFlow<String> = _planUsuario.asStateFlow()
    val empresaId: StateFlow<String?> = _empresaId.asStateFlow()
    val tipoUsuario: StateFlow<String?> = _tipoUsuario.asStateFlow()
    val displayName: StateFlow<String?> = _displayName.asStateFlow()
    val photoUrl: StateFlow<String?> = _photoUrl.asStateFlow()
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // ========== MÉTODOS GET EXISTENTES (preservados) ==========
    fun getIdioma(): String = _idioma.value
    fun getFuente(): String = _fuente.value
    fun getModoOscuro(): Boolean = _temaOscuro.value
    fun getTemaOscuro(): Boolean = _temaOscuro.value
    fun getIsPremium(): Boolean = _versionApp.value == 1
    fun getVersionApp(): Int = _versionApp.value
    fun getUsuarioEmail(): String? = _usuarioEmail.value
    fun getUsuarioId(): String? = _usuarioId.value

    // ✅ NUEVOS MÉTODOS GET PARA SISTEMA DE USUARIOS
    fun getPlanUsuario(): String = _planUsuario.value
    fun getEmpresaId(): String? = _empresaId.value
    fun getTipoUsuario(): String? = _tipoUsuario.value
    fun getDisplayName(): String? = _displayName.value
    fun getPhotoUrl(): String? = _photoUrl.value
    fun getIsAuthenticated(): Boolean = _isAuthenticated.value

    // ✅ MÉTODOS DE VERIFICACIÓN DE PERMISOS
    fun isPremium(): Boolean = _planUsuario.value == "PREMIUM"
    fun isFree(): Boolean = _planUsuario.value == "FREE"
    fun isSuperAdmin(): Boolean = _tipoUsuario.value == "SUPER_ADMIN"
    fun isAdmin(): Boolean = _tipoUsuario.value == "ADMIN" || _tipoUsuario.value == "SUPER_ADMIN"
    fun canManageUsers(): Boolean = _tipoUsuario.value == "SUPER_ADMIN" || _tipoUsuario.value == "ADMIN"
    fun canCreateContent(): Boolean = _tipoUsuario.value != "INVITADO" && _planUsuario.value == "PREMIUM"
    fun canDeleteContent(): Boolean = _tipoUsuario.value == "SUPER_ADMIN" || _tipoUsuario.value == "ADMIN"

    // ✅ MÉTODO DE VERIFICACIÓN DE SCOPE (para queries de datos)
    fun getCurrentScope(): DataScope {
        return when (_planUsuario.value) {
            "FREE" -> DataScope.Personal(_usuarioId.value ?: "usuario_default")
            "PREMIUM" -> DataScope.Empresa(_empresaId.value ?: "")
            else -> DataScope.Personal(_usuarioId.value ?: "usuario_default")
        }
    }

    // ✅ SEALED CLASS PARA SCOPE DE DATOS
    sealed class DataScope {
        data class Personal(val userId: String) : DataScope()
        data class Empresa(val empresaId: String) : DataScope()
    }

    // ========== MÉTODOS UPDATE EXISTENTES (preservados) ==========
    fun updateConfiguration(
        idioma: String,
        fuente: String,
        modoOscuro: Boolean,
        isPremium: Boolean
    ) {
        _idioma.value = idioma
        _fuente.value = fuente
        _temaOscuro.value = modoOscuro
        _versionApp.value = if (isPremium) 1 else 0
        // ✅ SINCRONIZAR con nuevo sistema
        _planUsuario.value = if (isPremium) "PREMIUM" else "FREE"
    }

    fun updateConfigurationComplete(
        idioma: String = _idioma.value,
        fuente: String = _fuente.value,
        temaOscuro: Boolean = _temaOscuro.value,
        versionApp: Int = _versionApp.value,
        usuarioEmail: String? = _usuarioEmail.value,
        usuarioId: String? = _usuarioId.value
    ) {
        _idioma.value = idioma
        _fuente.value = fuente
        _temaOscuro.value = temaOscuro
        _versionApp.value = versionApp
        _usuarioEmail.value = usuarioEmail
        _usuarioId.value = usuarioId
        // ✅ SINCRONIZAR con nuevo sistema
        _planUsuario.value = if (versionApp == 1) "PREMIUM" else "FREE"
    }

    // ✅ NUEVO MÉTODO UPDATE COMPLETO CON SISTEMA DE USUARIOS
    fun updateUserConfiguration(
        idioma: String = _idioma.value,
        fuente: String = _fuente.value,
        temaOscuro: Boolean = _temaOscuro.value,
        usuarioEmail: String? = _usuarioEmail.value,
        usuarioId: String? = _usuarioId.value,
        planUsuario: String = _planUsuario.value,
        empresaId: String? = _empresaId.value,
        tipoUsuario: String? = _tipoUsuario.value,
        displayName: String? = _displayName.value,
        photoUrl: String? = _photoUrl.value,
        isAuthenticated: Boolean = _isAuthenticated.value
    ) {
        // Configuración básica
        _idioma.value = idioma
        _fuente.value = fuente
        _temaOscuro.value = temaOscuro
        _usuarioEmail.value = usuarioEmail
        _usuarioId.value = usuarioId

        // Sistema de usuarios
        _planUsuario.value = planUsuario
        _empresaId.value = empresaId
        _tipoUsuario.value = tipoUsuario
        _displayName.value = displayName
        _photoUrl.value = photoUrl
        _isAuthenticated.value = isAuthenticated

        // ✅ SINCRONIZAR versionApp con planUsuario
        _versionApp.value = if (planUsuario == "PREMIUM") 1 else 0
    }

    // ========== MÉTODO RESET AMPLIADO ==========
    fun resetToDefaults() {
        // Estados existentes
        _idioma.value = "es"
        _fuente.value = "Montserrat"
        _temaOscuro.value = false
        _versionApp.value = 0
        _usuarioEmail.value = null
        _usuarioId.value = null

        // ✅ NUEVOS estados de usuario
        _planUsuario.value = "FREE"
        _empresaId.value = null
        _tipoUsuario.value = null
        _displayName.value = null
        _photoUrl.value = null
        _isAuthenticated.value = false
    }

    // ========== MÉTODOS SET EXISTENTES (preservados) ==========
    fun setIdioma(idioma: String) {
        _idioma.value = idioma
    }

    fun setFuente(fuente: String) {
        _fuente.value = fuente
    }

    fun setTemaOscuro(temaOscuro: Boolean) {
        _temaOscuro.value = temaOscuro
    }

    fun setModoOscuro(modoOscuro: Boolean) {
        _temaOscuro.value = modoOscuro
    }

    fun setIsPremium(isPremium: Boolean) {
        _versionApp.value = if (isPremium) 1 else 0
        // ✅ SINCRONIZAR con nuevo sistema
        _planUsuario.value = if (isPremium) "PREMIUM" else "FREE"
    }

    fun setVersionApp(versionApp: Int) {
        _versionApp.value = versionApp
        // ✅ SINCRONIZAR con nuevo sistema
        _planUsuario.value = if (versionApp == 1) "PREMIUM" else "FREE"
    }

    fun setUsuarioEmail(email: String?) {
        _usuarioEmail.value = email
    }

    fun setUsuarioId(userId: String?) {
        _usuarioId.value = userId
    }

    // ✅ NUEVOS MÉTODOS SET PARA SISTEMA DE USUARIOS
    fun setPlanUsuario(planUsuario: String) {
        _planUsuario.value = planUsuario
        // ✅ SINCRONIZAR con versionApp existente
        _versionApp.value = if (planUsuario == "PREMIUM") 1 else 0
    }

    fun setEmpresaId(empresaId: String?) {
        _empresaId.value = empresaId
    }

    fun setTipoUsuario(tipoUsuario: String?) {
        _tipoUsuario.value = tipoUsuario
    }

    fun setDisplayName(displayName: String?) {
        _displayName.value = displayName
    }

    fun setPhotoUrl(photoUrl: String?) {
        _photoUrl.value = photoUrl
    }

    fun setIsAuthenticated(isAuthenticated: Boolean) {
        _isAuthenticated.value = isAuthenticated

        // ✅ Si no está autenticado, limpiar datos de usuario
        if (!isAuthenticated) {
            _usuarioEmail.value = null
            _usuarioId.value = null
            _displayName.value = null
            _photoUrl.value = null
            _planUsuario.value = "FREE"
            _empresaId.value = null
            _tipoUsuario.value = null
            _versionApp.value = 0
        }
    }

    // ✅ MÉTODO PARA AUTENTICAR USUARIO (desde AuthRepository)
    fun authenticateUser(
        email: String?,
        userId: String?,
        displayName: String? = null,
        photoUrl: String? = null,
        planUsuario: String = "FREE",
        empresaId: String? = null,
        tipoUsuario: String? = null
    ) {
        _isAuthenticated.value = true
        _usuarioEmail.value = email
        _usuarioId.value = userId
        _displayName.value = displayName
        _photoUrl.value = photoUrl
        _planUsuario.value = planUsuario
        _empresaId.value = empresaId
        _tipoUsuario.value = tipoUsuario
        _versionApp.value = if (planUsuario == "PREMIUM") 1 else 0
    }

    // ✅ MÉTODO PARA LOGOUT USUARIO
    fun logoutUser() {
        _isAuthenticated.value = false
        _usuarioEmail.value = null
        _usuarioId.value = null
        _displayName.value = null
        _photoUrl.value = null
        _planUsuario.value = "FREE"
        _empresaId.value = null
        _tipoUsuario.value = null
        _versionApp.value = 0
    }

    // ========== MÉTODO GET CONFIGURATION AMPLIADO ==========
    fun getCurrentConfiguration(): Map<String, Any?> {
        return mapOf(
            // Configuración existente
            "idioma" to _idioma.value,
            "fuente" to _fuente.value,
            "temaOscuro" to _temaOscuro.value,
            "modoOscuro" to _temaOscuro.value, // alias para compatibilidad
            "versionApp" to _versionApp.value,
            "isPremium" to (_versionApp.value == 1),
            "usuarioEmail" to _usuarioEmail.value,
            "usuarioId" to _usuarioId.value,

            // ✅ NUEVOS campos de usuario
            "planUsuario" to _planUsuario.value,
            "empresaId" to _empresaId.value,
            "tipoUsuario" to _tipoUsuario.value,
            "displayName" to _displayName.value,
            "photoUrl" to _photoUrl.value,
            "isAuthenticated" to _isAuthenticated.value,

            // ✅ CAMPOS CALCULADOS
            "isFree" to (_planUsuario.value == "FREE"),
            "isPremiumPlan" to (_planUsuario.value == "PREMIUM"),
            "isSuperAdmin" to (_tipoUsuario.value == "SUPER_ADMIN"),
            "isAdmin" to (_tipoUsuario.value == "ADMIN" || _tipoUsuario.value == "SUPER_ADMIN"),
            "canManageUsers" to (_tipoUsuario.value == "SUPER_ADMIN" || _tipoUsuario.value == "ADMIN"),
            "currentScope" to getCurrentScope()
        )
    }

    // ✅ MÉTODO PARA DEBUG (útil para desarrollo)
    fun printCurrentState() {
        println("🔍 ConfigurationManager Estado Actual:")
        println("   - isAuthenticated: ${_isAuthenticated.value}")
        println("   - usuarioEmail: ${_usuarioEmail.value}")
        println("   - planUsuario: ${_planUsuario.value}")
        println("   - empresaId: ${_empresaId.value}")
        println("   - tipoUsuario: ${_tipoUsuario.value}")
        println("   - currentScope: ${getCurrentScope()}")
        println("   - idioma: ${_idioma.value}")
        println("   - fuente: ${_fuente.value}")
        println("   - temaOscuro: ${_temaOscuro.value}")
    }
}

//package es.nuskysoftware.marketsales.utils
//
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
///**
// * Singleton global para gestionar configuración dinámica sin @Composable
// */
//object ConfigurationManager {
//
//    // Estados internos
//    private val _idioma = MutableStateFlow("es")
//    private val _fuente = MutableStateFlow("Montserrat")
//    private val _temaOscuro = MutableStateFlow(false)
//    private val _versionApp = MutableStateFlow(0) // 0=FREE, 1=PREMIUM
//    private val _usuarioEmail = MutableStateFlow<String?>(null)
//    private val _usuarioId = MutableStateFlow<String?>(null)
//
//    // Estados públicos de solo lectura
//    val idioma: StateFlow<String> = _idioma.asStateFlow()
//    val fuente: StateFlow<String> = _fuente.asStateFlow()
//    val temaOscuro: StateFlow<Boolean> = _temaOscuro.asStateFlow()
//    val versionApp: StateFlow<Int> = _versionApp.asStateFlow()
//    val usuarioEmail: StateFlow<String?> = _usuarioEmail.asStateFlow()
//    val usuarioId: StateFlow<String?> = _usuarioId.asStateFlow()
//
//    // ✅ MÉTODOS GET INDIVIDUALES
//    fun getIdioma(): String = _idioma.value
//    fun getFuente(): String = _fuente.value
//    fun getModoOscuro(): Boolean = _temaOscuro.value
//    fun getTemaOscuro(): Boolean = _temaOscuro.value
//    fun getIsPremium(): Boolean = _versionApp.value == 1
//    fun getVersionApp(): Int = _versionApp.value
//    fun getUsuarioEmail(): String? = _usuarioEmail.value
//    fun getUsuarioId(): String? = _usuarioId.value
//
//    // ✅ MÉTODO UPDATE CONFIGURATION (compatibilidad con código existente)
//    fun updateConfiguration(
//        idioma: String,
//        fuente: String,
//        modoOscuro: Boolean,
//        isPremium: Boolean
//    ) {
//        _idioma.value = idioma
//        _fuente.value = fuente
//        _temaOscuro.value = modoOscuro
//        _versionApp.value = if (isPremium) 1 else 0
//    }
//
//    // ✅ MÉTODO UPDATE CONFIGURATION COMPLETO (con tu estructura)
//    fun updateConfigurationComplete(
//        idioma: String = _idioma.value,
//        fuente: String = _fuente.value,
//        temaOscuro: Boolean = _temaOscuro.value,
//        versionApp: Int = _versionApp.value,
//        usuarioEmail: String? = _usuarioEmail.value,
//        usuarioId: String? = _usuarioId.value
//    ) {
//        _idioma.value = idioma
//        _fuente.value = fuente
//        _temaOscuro.value = temaOscuro
//        _versionApp.value = versionApp
//        _usuarioEmail.value = usuarioEmail
//        _usuarioId.value = usuarioId
//    }
//
//    // ✅ MÉTODO RESET TO DEFAULTS
//    fun resetToDefaults() {
//        _idioma.value = "es"
//        _fuente.value = "Montserrat"
//        _temaOscuro.value = false
//        _versionApp.value = 0
//        _usuarioEmail.value = null
//        _usuarioId.value = null
//    }
//
//    // Métodos individuales de actualización
//    fun setIdioma(idioma: String) {
//        _idioma.value = idioma
//    }
//
//    fun setFuente(fuente: String) {
//        _fuente.value = fuente
//    }
//
//    fun setTemaOscuro(temaOscuro: Boolean) {
//        _temaOscuro.value = temaOscuro
//    }
//
//    fun setModoOscuro(modoOscuro: Boolean) {
//        _temaOscuro.value = modoOscuro
//    }
//
//    fun setIsPremium(isPremium: Boolean) {
//        _versionApp.value = if (isPremium) 1 else 0
//    }
//
//    fun setVersionApp(versionApp: Int) {
//        _versionApp.value = versionApp
//    }
//
//    fun setUsuarioEmail(email: String?) {
//        _usuarioEmail.value = email
//    }
//
//    fun setUsuarioId(userId: String?) {
//        _usuarioId.value = userId
//    }
//
//    // Obtener toda la configuración como mapa
//    fun getCurrentConfiguration(): Map<String, Any?> {
//        return mapOf(
//            "idioma" to _idioma.value,
//            "fuente" to _fuente.value,
//            "temaOscuro" to _temaOscuro.value,
//            "modoOscuro" to _temaOscuro.value, // alias para compatibilidad
//            "versionApp" to _versionApp.value,
//            "isPremium" to (_versionApp.value == 1),
//            "usuarioEmail" to _usuarioEmail.value,
//            "usuarioId" to _usuarioId.value
//        )
//    }
//}