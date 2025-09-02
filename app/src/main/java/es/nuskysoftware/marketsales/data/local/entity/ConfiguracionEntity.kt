    // app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/ConfiguracionEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ConfiguracionEntity V10 - SIMPLIFICADO MONOUSUARIO
 *
 * CAMBIOS V10:
 * - Configuración GLOBAL (moneda, idioma, fuente, tema) común a todos los usuarios
 * - usuarioLogueado: UID del usuario actual o "usuario_default"
 * - Eliminado: versionApp, empresaId, tipoUsuario, etc. (lógica multiusuario)
 * - Agregado: Campos de sincronización (version, lastModified, pendienteSync)
 */
@Entity(tableName = "configuracion")
data class ConfiguracionEntity(
    @PrimaryKey val id: Int = 1,

    // ========== CONFIGURACIÓN GLOBAL (común a todos los usuarios) ==========
    val moneda: String = "€ Euro",           // Común a todos
    val idioma: String = "es",               // Común a todos
    val fuente: String = "Montserrat",       // Común a todos
    val temaOscuro: Boolean = false,         // Común a todos

    // ========== USUARIO ACTUAL ==========
    val usuarioLogueado: String = "usuario_default",  // UID actual o "usuario_default"

    // ========== CAMPOS LEGACY (mantener para compatibilidad) ==========
    val numeroVersion: String = "V10.0",
    val ultimoDispositivo: String? = android.os.Build.MODEL,
    val fechaUltimaSync: String? = null,

    // ========== SINCRONIZACIÓN V10 ==========
    val version: Long = 1,
    val lastModified: Long = System.currentTimeMillis(),
    val pendienteSync: Boolean = false,
    val sincronizadoFirebase: Boolean = false
)

// ========== EXTENSION PROPERTIES (compatibilidad con código existente) ==========

/**
 * Determina si el usuario actual es Premium
 * NOTA: Ahora se debe consultar desde la tabla usuarios
 */
@Deprecated("Usar UserRepository.getCurrentUser().esPremium", ReplaceWith("userRepository.getCurrentUser()?.esPremium ?: false"))
val ConfiguracionEntity.isPremium: Boolean
    get() = false // Siempre false, se consulta desde usuarios

/**
 * Alias para compatibilidad
 */
val ConfiguracionEntity.modoOscuro: Boolean
    get() = this.temaOscuro

/**
 * Indica si es usuario por defecto
 */
val ConfiguracionEntity.isUsuarioDefault: Boolean
    get() = this.usuarioLogueado == "usuario_default"
