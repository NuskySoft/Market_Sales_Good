// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/UserEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ✅ COMPLETO V10 - UserEntity con todos los campos necesarios
 */
@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val esPremium: Boolean = false,        // Premium/Free

    // ✅ CAMPOS FALTANTES AGREGADOS
    val version: Long = 1,                 // Para versionado
    val lastModified: Long = System.currentTimeMillis(), // Para sincronización

    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaUltimaSync: String? = null,
    val sincronizadoFirebase: Boolean = false,
    val activo: Boolean = true
)
