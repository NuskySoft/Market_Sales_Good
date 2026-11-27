// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/UserEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Perfil de cuenta autenticada y datos de plan de usuario persistidos en Room.

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ✅ COMPLETO V10 - UserEntity con todos los campos necesarios
 */
@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val esPremium: Boolean = false,        // Premium/Free

    // ✅ CAMPOS FALTANTES AGREGADOS
    val version: Long = 1,                 // Para versionado
    val lastModified: Long = System.currentTimeMillis(), // Para sincronización

    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaUltimaSync: String? = null,
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false,
    // Marca de actividad; permite distinguir datos vigentes de bajas.
    val activo: Boolean = true
)
