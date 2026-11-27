package es.nuskysoftware.marketsales.data

// Finalidad de las pruebas: Regresiones de movimientos de stock, persistencia, recuperación y migración.

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.*
import es.nuskysoftware.marketsales.data.repository.*
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StockMovementTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_18_19, AppDatabase.MIGRATION_19_20).build()

    private fun product(id: String = "item", quantity: Int = 2) = LineaVentaUI(
        tipoLinea = TipoLinea.PRODUCTO, descripcion = "Producto", idProducto = id,
        cantidad = quantity, precioUnitario = 1.0, subtotal = quantity.toDouble()
    )
    private fun manual() = LineaVentaUI(
        tipoLinea = TipoLinea.MANUAL, descripcion = "Concepto manual", cantidad = 1,
        precioUnitario = 3.0, subtotal = 3.0
    )
    private suspend fun seed(db: AppDatabase) {
        db.mercadilloDao().insertMercadillo(MercadilloEntity(idMercadillo = "market", userId = "stock-test"))
        db.articuloDao().insertArticulo(ArticuloEntity(idArticulo = "item", userId = "stock-test", stock = 10, controlarStock = true))
    }
    private suspend fun <T> asPremium(block: suspend () -> T): T {
        val owner = ConfigurationManager.getCurrentUserId()
        val premium = ConfigurationManager.getIsPremium()
        ConfigurationManager.setUsuarioLogueado("stock-test")
        ConfigurationManager.setPremiumForDevelopment(true)
        return try { block() } finally {
            ConfigurationManager.setUsuarioLogueado(owner)
            ConfigurationManager.setPremiumForDevelopment(premium)
        }
    }

    @Test fun committedSaleSurvivesStockFailureAndReopeningWithoutDoubleDiscount() = runBlocking {
        asPremium {
            val name = "stock-${UUID.randomUUID()}.db"
            var db = open(name)
            try {
                seed(db)
                // Simulate a failure after COMMIT, before any background work can be scheduled.
                val sale = VentasRepository(context, db) { throw IllegalStateException("Scheduler unavailable") }
                    .guardarVenta("market", listOf(product(), manual()), "EFECTIVO", 5.0)
                assertTrue(sale.isSuccess)
                assertNotNull(db.recibosDao().obtenerReciboPorId(sale.getOrThrow()))
                assertEquals(10, db.articuloDao().getArticuloById("item")!!.stock)
                assertEquals(1, db.stockMovementDao().pendingCount())

                // Fail BETWEEN the stock UPDATE and its acknowledgement.
                db.openHelper.writableDatabase.execSQL("""
                    CREATE TRIGGER fail_ack BEFORE UPDATE OF aplicado ON stock_movements
                    BEGIN SELECT RAISE(ABORT, 'simulated interruption'); END
                """.trimIndent())
                assertFalse(StockMovementProcessor(db).processPending())
                assertEquals(10, db.articuloDao().getArticuloById("item")!!.stock)
                assertNotNull(db.recibosDao().obtenerReciboPorId(sale.getOrThrow()))
                assertEquals(1, db.stockMovementDao().pendingCount())
                db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_ack")

                // Reopening the database/recreating the processor simulates recovery, not a new sale.
                db.close()
                db = open(name)
                coroutineScope {
                    (1..4).map { async(Dispatchers.IO) { StockMovementProcessor(db).processPending() } }.awaitAll()
                }
                assertEquals(8, db.articuloDao().getArticuloById("item")!!.stock)
                assertEquals(0, db.stockMovementDao().pendingCount())
                StockMovementProcessor(db).processPending()
                assertEquals(8, db.articuloDao().getArticuloById("item")!!.stock)
                assertEquals(1, db.recibosDao().contarPorUsuario("stock-test"))
            } finally { db.close(); context.deleteDatabase(name) }
        }
    }

    @Test fun failedMovementDoesNotBlockOtherProductsAndNeverUpdatesAnotherOwner() = runBlocking {
        asPremium {
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                seed(db)
                db.articuloDao().insertArticulo(ArticuloEntity(idArticulo = "foreign", userId = "other-owner", stock = 20, controlarStock = true))
                val repo = VentasRepository(context, db) { }
                assertTrue(repo.guardarVenta("market", listOf(product("foreign"), product()), "EFECTIVO", 4.0).isSuccess)
                assertFalse(StockMovementProcessor(db).processPending())
                assertEquals(8, db.articuloDao().getArticuloById("item")!!.stock)
                assertEquals(20, db.articuloDao().getArticuloById("foreign")!!.stock)
                assertEquals(1, db.stockMovementDao().pendingCount())
                // Recovery of the missing/right-owner article completes only the remaining movement.
                db.articuloDao().insertArticulo(ArticuloEntity(idArticulo = "foreign", userId = "stock-test", stock = 20, controlarStock = true))
                assertTrue(StockMovementProcessor(db).processPending())
                assertEquals(18, db.articuloDao().getArticuloById("foreign")!!.stock)
                assertEquals(8, db.articuloDao().getArticuloById("item")!!.stock)
            } finally { db.close() }
        }
    }

    @Test fun freeAndStockDisabledProductsNeverAcquireADiscountObligation() = runBlocking {
        asPremium {
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                seed(db)
                val repo = VentasRepository(context, db) { }
                ConfigurationManager.setPremiumForDevelopment(false)
                assertTrue(repo.guardarVenta("market", listOf(product()), "EFECTIVO", 2.0).isSuccess)
                ConfigurationManager.setPremiumForDevelopment(true)
                val item = db.articuloDao().getArticuloById("item")!!
                db.articuloDao().updateArticulo(item.copy(controlarStock = false))
                assertTrue(repo.guardarVenta("market", listOf(product(), manual()), "EFECTIVO", 5.0).isSuccess)
                assertEquals(0, db.stockMovementDao().pendingCount())
                assertEquals(10, db.articuloDao().getArticuloById("item")!!.stock)
            } finally { db.close() }
        }
    }

    @Test fun duplicateEnqueueAndConcurrentProcessorsApplyEachSaleOnce() = runBlocking {
        asPremium {
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                seed(db)
                val repo = VentasRepository(context, db) { }
                assertTrue(repo.guardarVenta("market", listOf(product()), "EFECTIVO", 2.0).isSuccess)
                assertTrue(repo.guardarVenta("market", listOf(product()), "BIZUM", 2.0).isSuccess)
                val originals = db.stockMovementDao().pending()
                db.stockMovementDao().enqueue(originals)
                coroutineScope {
                    (1..5).map { async(Dispatchers.IO) { StockMovementProcessor(db).processPending() } }.awaitAll()
                }
                assertEquals(6, db.articuloDao().getArticuloById("item")!!.stock)
                db.stockMovementDao().enqueue(originals) // IGNORE must not reset applied=true.
                StockMovementProcessor(db).processPending()
                assertEquals(6, db.articuloDao().getArticuloById("item")!!.stock)
                assertEquals(2, db.recibosDao().contarPorUsuario("stock-test"))
            } finally { db.close() }
        }
    }

    @Test fun oldUploadAcknowledgementKeepsNewStockPending() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            seed(db)
            val old = db.articuloDao().getArticuloById("item")!!
            db.stockMovementDao().enqueue(listOf(StockMovementEntity("movement", "stock-test", "sale", "item", 2, 1L)))
            assertTrue(StockMovementProcessor(db).processPending())
            db.articuloDao().marcarSnapshotSincronizado(old.idArticulo, old.userId, old.version, old.lastModified, old.stock)
            assertFalse(db.articuloDao().getArticuloById("item")!!.sincronizadoFirebase)
            val current = db.articuloDao().getArticuloById("item")!!
            db.articuloDao().marcarSnapshotSincronizado(current.idArticulo, current.userId, current.version, current.lastModified, current.stock)
            assertTrue(db.articuloDao().getArticuloById("item")!!.sincronizadoFirebase)
        } finally { db.close() }
    }

    @Test fun upgradeFrom18PreservesExistingDataAndDoesNotReplayOldSales() = runBlocking {
        val name = "migration-stock-${UUID.randomUUID()}.db"
        var db = open(name)
        try {
            seed(db)
            db.recibosDao().insertarRecibo(ReciboEntity(idRecibo = "old-sale", idMercadillo = "market", idUsuario = "stock-test", fechaHora = 1L, metodoPago = "EFECTIVO", totalTicket = 2.0))
            // Recreate the v18 table set; v18 has no stock_movements table.
            db.openHelper.writableDatabase.execSQL("DROP TABLE stock_movements")
            db.openHelper.writableDatabase.execSQL("PRAGMA user_version = 18")
            db.close()
            db = open(name)
            assertEquals(10, db.articuloDao().getArticuloById("item")!!.stock)
            assertNotNull(db.recibosDao().obtenerReciboPorId("old-sale"))
            assertEquals(0, db.stockMovementDao().pendingCount())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
