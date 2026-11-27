// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/CategoriaEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Categoría del catálogo identificada por cuenta; incluye la marca de actividad y los metadatos de cambios.

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * CategoriaEntity V11 - Market Sales
 *
 * CAMBIOS RESPECTO A CAJA MERCADILLOS:
 * - Agregado campo userId para vincular con tabla usuarios
 * - Compatible con arquitectura híbrida "Reloj Suizo"
 * - Sincronización con Firebase + Room offline-first
 */
@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey
    // Identificador de la categoría.
    val idCategoria: String = UUID.randomUUID().toString(),

    // ✅ NUEVO: Campo para vincular con usuario
    // Identificador de la cuenta propietaria.
    val userId: String = "",

    val nombre: String = "",
    val colorHex: String = "#FFFFFF",
    val orden: Int = 0,
    val activa: Boolean = true,

    // Campos de sincronización híbrida
    // Revisión del registro utilizada para el control de cambios.
    val version: Long = 1,
    // Marca temporal de la modificación; no debe confundirse con la hora de subida.
    val lastModified: Long = System.currentTimeMillis(),
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false
) {
    // Constructor vacío para Firestore
    constructor() : this(
        idCategoria = "",
        userId = "",
        nombre = "",
        colorHex = "#FFFFFF",
        orden = 0,
        activa = true,
        version = 1,
        lastModified = System.currentTimeMillis(),
        sincronizadoFirebase = false
    )
}