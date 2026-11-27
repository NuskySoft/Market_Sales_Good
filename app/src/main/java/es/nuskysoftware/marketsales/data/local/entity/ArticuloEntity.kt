// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/ArticuloEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Artículo del catálogo: identidad, categoría, precio, existencias y datos de sincronización.

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * ArticuloEntity V11 - Market Sales
 *
 * CAMBIOS RESPECTO A CAJA MERCADILLOS:
 * - Agregado campo userId para vincular con usuario
 * - Compatible con arquitectura híbrida "Reloj Suizo"
 * - Sincronización con Firebase + Room offline-first
 * - Campos de control Premium (coste, stock)
 */
@Entity(tableName = "articulos")
data class ArticuloEntity(
    @PrimaryKey
    // Identificador estable del artículo.
    val idArticulo: String = UUID.randomUUID().toString(),

    // ✅ NUEVO: Campo para vincular con usuario
    // Identificador de la cuenta propietaria.
    val userId: String = "",

    // Campos principales
    val nombre: String = "",
    // Identificador de la categoría.
    val idCategoria: String = "",
    val precioVenta: Double = 0.0,

    // Campos Premium
    val precioCoste: Double? = null,
    // Existencias registradas del artículo.
    val stock: Int? = null,
    val stockBase: Int? = stock,
    val controlarStock: Boolean = false,
    val controlarCoste: Boolean = false,

    // Campos adicionales
    val favorito: Boolean = false,
    val fotoUri: String? = null,
    // Marca de actividad; permite distinguir datos vigentes de bajas.
    val activo: Boolean = true,

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
        idArticulo = "",
        userId = "",
        nombre = "",
        idCategoria = "",
        precioVenta = 0.0,
        precioCoste = null,
        stock = null,
        controlarStock = false,
        controlarCoste = false,
        favorito = false,
        fotoUri = null,
        activo = true,
        version = 1,
        lastModified = System.currentTimeMillis(),
        sincronizadoFirebase = false
    )
}
