package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val planUsuario: String = "FREE",               // FREE, PREMIUM
    val empresaId: String? = null,                  // NULL para FREE, empresaId para PREMIUM
    val tipoUsuario: String? = null,                // NULL para FREE, tipos para PREMIUM
    val permisos: String? = null,                   // JSON string con permisos (NULL para FREE)
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaUltimaSync: String? = null,
    val pendienteSync: Boolean = false,
    val activo: Boolean = true
)

// ✅ EXTENSION PROPERTIES para compatibilidad y fácil acceso
val UserEntity.isPremium: Boolean
    get() = this.planUsuario == "PREMIUM"

val UserEntity.isFree: Boolean
    get() = this.planUsuario == "FREE"

val UserEntity.isSuperAdmin: Boolean
    get() = this.tipoUsuario == "SUPER_ADMIN"

val UserEntity.isAdmin: Boolean
    get() = this.tipoUsuario == "ADMIN" || this.tipoUsuario == "SUPER_ADMIN"

val UserEntity.canManageUsers: Boolean
    get() = this.tipoUsuario == "SUPER_ADMIN" || this.tipoUsuario == "ADMIN"

val UserEntity.canCreateContent: Boolean
    get() = this.tipoUsuario != "INVITADO" && this.planUsuario == "PREMIUM"

// ✅ CORRECCIÓN: Extension para parsear permisos SIN GSON (por ahora simple)
val UserEntity.hasPermission: (String, String) -> Boolean
    get() = { module: String, action: String ->
        // Implementación simple por ahora, después podemos usar JSON
        when (tipoUsuario) {
            "SUPER_ADMIN" -> true
            "ADMIN" -> action != "eliminar" // Admin no puede eliminar
            "EMPLEADO" -> action == "ver" || action == "crear" // Solo ver y crear
            "INVITADO" -> action == "ver" // Solo ver
            else -> planUsuario == "FREE" // FREE tiene acceso básico a todo
        }
    }
