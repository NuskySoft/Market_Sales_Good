package es.nuskysoftware.marketsales.data

// Finalidad de las pruebas: Regresión Room de clave de gasto compuesta y confirmación de la revisión enviada.

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GastosSyncTest {
    @Test
    fun confirmarGastoNoConfirmaOtraLineaNiUnaEdicionPosterior() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        try {
            val dao = db.lineasGastosDao()
            val first = LineaGastoEntity(
                idMercadillo = "market-1", idUsuario = "alice", numeroLinea = "0001",
                descripcion = "Gasto 1", importe = 5.0, fechaHora = 100L,
                formaPago = "efectivo", descontarDeCaja = false, lastModified = 100L
            )
            val second = first.copy(idMercadillo = "market-2", importe = 8.0)
            val other = first.copy(idMercadillo = "market-3", idUsuario = "bob")
            dao.insertarLineas(listOf(first, second, other))
            assertEquals(5.0, dao.getTotalGastosMercadillo(first.idMercadillo), 0.0)
            assertEquals(8.0, dao.getTotalGastosMercadillo(second.idMercadillo), 0.0)
            assertEquals(0, dao.marcarSincronizado("bob", first.idMercadillo, "0001", 100L, 1L))
            assertEquals(1, dao.marcarSincronizado("alice", first.idMercadillo, "0001", 100L, 1L))
            assertEquals(listOf(second), dao.getPendientesSync("alice"))
            assertEquals(listOf(other), dao.getPendientesSync("bob"))
            val edited = first.copy(lastModified = 101L, version = 2L, importe = 6.0)
            dao.insertarLinea(edited)
            assertEquals(0, dao.marcarSincronizado("alice", first.idMercadillo, "0001", 100L, 1L))
            assertEquals(edited, dao.getPendientesSync("alice").first { it.idMercadillo == first.idMercadillo })
        } finally {
            db.close()
        }
    }
}
