// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/ArticuloEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

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
    val idArticulo: String = UUID.randomUUID().toString(),

    // ✅ NUEVO: Campo para vincular con usuario
    val userId: String = "",

    // Campos principales
    val nombre: String = "",
    val idCategoria: String = "",
    val precioVenta: Double = 0.0,

    // Campos Premium
    val precioCoste: Double? = null,
    val stock: Int? = null,
    val controlarStock: Boolean = false,
    val controlarCoste: Boolean = false,

    // Campos adicionales
    val favorito: Boolean = false,
    val fotoUri: String? = null,
    val activo: Boolean = true,

    // Campos de sincronización híbrida
    val version: Long = 1,
    val lastModified: Long = System.currentTimeMillis(),
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