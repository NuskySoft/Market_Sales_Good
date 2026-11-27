package es.nuskysoftware.marketsales.data.local.entity

// Responsabilidad: Datos fiscales y de contacto de la empresa asociada a un propietario.

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * EmpresaEntity - Datos de empresa con sincronización híbrida
 */
@Entity(tableName = "empresa", primaryKeys = ["userId"])
data class EmpresaEntity(
    val id: Int = 1,
    // Identificador de la cuenta propietaria.
    val userId: String = "",
    val nif: String = "",
    val nombre: String = "",
    val razonSocial: String = "",
    val direccion: String = "",
    val poblacion: String = "",
    val codigoPostal: String = "",
    val provincia: String = "",
    val pais: String = "",
    // Revisión del registro utilizada para el control de cambios.
    val version: Long = 1,
    // Marca temporal de la modificación; no debe confundirse con la hora de subida.
    val lastModified: Long = System.currentTimeMillis(),
    // Indica si esta revisión local está confirmada en Firebase.
    val sincronizadoFirebase: Boolean = false
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this(userId = "")
}
