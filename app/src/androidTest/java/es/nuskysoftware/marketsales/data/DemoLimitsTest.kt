package es.nuskysoftware.marketsales.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.data.repository.*
import es.nuskysoftware.marketsales.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoLimitsTest {
    @Test fun concurrentSalesStopAtTenAndAccountsStaySeparate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val old = ConfigurationManager.getCurrentUserId()
        ConfigurationManager.setUsuarioLogueado(AppModePolicy.DEMO_USER_ID)
        try {
            val market = MercadilloEntity(idMercadillo = "demo", userId = AppModePolicy.DEMO_USER_ID, estado = 7)
            db.mercadilloDao().insertMercadillo(market)
            val repo = VentasRepository(context, db)
            val line = LineaVentaUI(tipoLinea = TipoLinea.MANUAL, descripcion = "Prueba", cantidad = 1, precioUnitario = 1.0, subtotal = 1.0)
            val results = coroutineScope {
                (1..12).map { async(Dispatchers.IO) { repo.guardarVenta("demo", listOf(line, line), "EFECTIVO", 2.0) } }.awaitAll()
            }
            assertEquals(10, results.count { it.isSuccess })
            assertEquals(10, db.recibosDao().contarDemo(AppModePolicy.DEMO_USER_ID))
            assertEquals(20, db.lineasVentaDao().obtenerLineasPorMercadillo("demo").first().size)
            assertEquals(10, results.mapNotNull { it.getOrNull() }.toSet().size)
            assertEquals(0, db.recibosDao().contarDemo("new-account"))
            // A new repository (reopening screen) must not reset persisted limits.
            assertTrue(VentasRepository(context, db).guardarVenta("demo", listOf(line), "EFECTIVO", 1.0).isFailure)
            assertFalse(AppModePolicy.canSync(market.userId, "new-account"))
        } finally {
            ConfigurationManager.setUsuarioLogueado(old)
            DemoAccess.dismiss()
            db.close()
        }
    }
    @Test fun singleMarketCategoryAndProductLimitsAreCheckedAtWrite() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val old = ConfigurationManager.getCurrentUserId()
        ConfigurationManager.setUsuarioLogueado(AppModePolicy.DEMO_USER_ID)
        try {
            val categories = CategoriaRepository(context, db)
            val categoryId = categories.crearCategoria("Demo", "#ffffff")
            assertTrue(runCatching { categories.crearCategoria("Segunda", "#ffffff") }.isFailure)
            val products = ArticuloRepository(context, db)
            products.crearArticulo(nombre = "Demo", idCategoria = categoryId, precioVenta = 1.0)
            assertTrue(runCatching { products.crearArticulo(nombre = "Otro", idCategoria = categoryId, precioVenta = 1.0) }.isFailure)
            val markets = MercadilloRepository(context, db)
            val id = markets.crearMercadillo("01-01-2030", "Demo", "Demo")
            assertTrue(runCatching { markets.crearMercadillo("02-01-2030", "Otro", "Otro") }.isFailure)
            assertTrue(runCatching { markets.desactivarMercadillo(id) }.isFailure)
            assertEquals(1, db.mercadilloDao().contarDemo(AppModePolicy.DEMO_USER_ID))
            assertEquals(0, db.mercadilloDao().contarDemo("new-account"))
        } finally {
            ConfigurationManager.setUsuarioLogueado(old)
            DemoAccess.dismiss()
            db.close()
        }
    }
}
