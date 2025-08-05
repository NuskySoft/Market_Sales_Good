// app/src/main/java/es/nuskysoftware/marketsales/data/local/entity/ConfiguracionEntity.kt
package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracion")
data class ConfiguracionEntity(
    @PrimaryKey val id: Int = 1,
    val versionApp: Int = 0,           // 0=FREE, 1=PREMIUM
    val numeroVersion: String = "V1.0",
    val ultimoDispositivo: String? = null,
    val usuarioEmail: String? = null,
    val usuarioId: String? = null,
    val usuarioPassword: String? = null,
    val idioma: String = "es",         // es/en
    val temaOscuro: Boolean = false,   // tema dinámico
    val fuente: String = "Montserrat", // fuentes dinámicas
    val moneda: String = "€ Euro",
    val fechaUltimaSync: String? = null,
    /** true = hay cambios locales pendientes de enviar a Firebase */
    val pendienteSync: Boolean = false
)

// EXTENSION PROPERTIES para compatibilidad con código existente
val ConfiguracionEntity.modoOscuro: Boolean
    get() = this.temaOscuro

val ConfiguracionEntity.isPremium: Boolean
    get() = this.versionApp == 1

