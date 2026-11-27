package es.nuskysoftware.marketsales.data.repository

// Responsabilidad: Construye la identidad compuesta y la representación remota común de los gastos.

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.nuskysoftware.marketsales.data.local.dao.LineasGastosDao
import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import es.nuskysoftware.marketsales.utils.DemoAccess
import kotlinx.coroutines.tasks.await

/** Una sola representación y confirmación de gastos, para todas las rutas de subida. */
internal object GastoSync {
    // Forma la clave mercadillo-línea para distinguir numeraciones iguales de mercadillos diferentes.
    fun documentId(gasto: LineaGastoEntity): String = "${gasto.idMercadillo}-${gasto.numeroLinea}"

    // Serializa los campos de negocio y sus metadatos de sincronización.
    fun fields(gasto: LineaGastoEntity): Map<String, Any> = mapOf(
        "idMercadillo" to gasto.idMercadillo,
        "idUsuario" to gasto.idUsuario,
        "numeroLinea" to gasto.numeroLinea,
        "descripcion" to gasto.descripcion,
        "importe" to gasto.importe,
        "fechaHora" to gasto.fechaHora,
        "formaPago" to gasto.formaPago,
        "descontarDeCaja" to gasto.descontarDeCaja,
        "activo" to gasto.activo,
        "version" to gasto.version,
        "lastModified" to gasto.lastModified,
        "fechaSync" to System.currentTimeMillis()
    )

}
