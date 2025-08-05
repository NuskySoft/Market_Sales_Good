// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/CategoriaEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

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
    val idCategoria: String = UUID.randomUUID().toString(),

    // ✅ NUEVO: Campo para vincular con usuario
    val userId: String = "",

    val nombre: String = "",
    val colorHex: String = "#FFFFFF",
    val orden: Int = 0,
    val activa: Boolean = true,

    // Campos de sincronización híbrida
    val version: Long = 1,
    val lastModified: Long = System.currentTimeMillis(),
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