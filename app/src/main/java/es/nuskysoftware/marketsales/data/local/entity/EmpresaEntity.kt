package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * EmpresaEntity - Datos de empresa con sincronización híbrida
 */
@Entity(tableName = "empresa")
data class EmpresaEntity(
    @PrimaryKey val id: Int = 1,
    val nif: String = "",
    val nombre: String = "",
    val razonSocial: String = "",
    val direccion: String = "",
    val poblacion: String = "",
    val codigoPostal: String = "",
    val provincia: String = "",
    val pais: String = "",
    val version: Long = 1,
    val lastModified: Long = System.currentTimeMillis(),
    val sincronizadoFirebase: Boolean = false
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this(1, "", "", "", "", "", "", "", "", 1, System.currentTimeMillis(), false)
}