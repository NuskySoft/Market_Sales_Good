package es.nuskysoftware.marketsales.data.repository

import android.util.Log
import androidx.room.withTransaction
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import kotlinx.coroutines.CancellationException

/** No network or UI dependencies. A failure never rolls back an already confirmed sale. */
class StockMovementProcessor(private val db: AppDatabase) {
    suspend fun processPending(owner: String? = null): Boolean {
        val dao = db.stockMovementDao()
        for (candidate in dao.pending().filter { owner == null || it.idUsuario == owner }) {
            try {
                db.withTransaction {
                    // Another worker may have applied it while we waited for the transaction.
                    val movement = dao.byId(candidate.idMovimiento)
                    if (movement != null && !movement.aplicado) {
                        check(dao.applyStock(movement.idArticulo, movement.idUsuario, movement.cantidad, System.currentTimeMillis()) == 1) {
                            "Article missing or belongs to another owner: ${movement.idMovimiento}"
                        }
                        // Stock and acknowledgement commit together, so a retry cannot apply twice.
                        dao.markApplied(movement.idMovimiento)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w("StockMovementProcessor", "Stock remains pending: ${candidate.idMovimiento}", failure)
            }
        }
        return dao.pending().none { owner == null || it.idUsuario == owner }
    }
}
