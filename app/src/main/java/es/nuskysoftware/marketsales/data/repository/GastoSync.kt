package es.nuskysoftware.marketsales.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.nuskysoftware.marketsales.data.local.dao.LineasGastosDao
import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import es.nuskysoftware.marketsales.utils.DemoAccess
import kotlinx.coroutines.tasks.await

/** Una sola representación y confirmación de gastos, para todas las rutas de subida. */
internal object GastoSync {
    fun documentId(gasto: LineaGastoEntity): String = "${gasto.idMercadillo}-${gasto.numeroLinea}"

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

    suspend fun push(fs: FirebaseFirestore, dao: LineasGastosDao, gasto: LineaGastoEntity) {
        if (!DemoAccess.canSync(gasto.idUsuario)) return
        fs.collection("LineasGastos").document(documentId(gasto))
            .set(fields(gasto), SetOptions.merge()).await()
        dao.marcarSincronizado(
            gasto.idUsuario, gasto.idMercadillo, gasto.numeroLinea,
            gasto.lastModified, gasto.version
        )
    }
}
