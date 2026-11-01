package es.nuskysoftware.marketsales.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MercadilloCanceladoTest {
    @Test
    fun cancelarConservaLaFichaYLaMantieneEnElCalendario() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = database.mercadilloDao()
            val original = MercadilloEntity(
                idMercadillo = "cancelado",
                userId = "usuario_prueba",
                fecha = "01-11-2026",
                lugar = "Cartagena",
                organizador = "Organizador de prueba",
                esGratis = false,
                importeSuscripcion = 25.0,
                requiereMesa = false,
                requiereCarpa = true,
                hayPuntoLuz = true,
                horaInicio = "10:30",
                horaFin = "18:45",
                estado = EstadosMercadillo.Estado.EN_CURSO.codigo,
                saldoInicial = 100.0,
                totalGastos = 12.5,
                sincronizadoFirebase = true
            )
            val otro = original.copy(idMercadillo = "otro", lugar = "Otro mercadillo")
            dao.insertMercadillo(original)
            dao.insertMercadillo(otro)

            dao.cancelarMercadillo(original.idMercadillo, timestamp = 123456L)

            val esperado = original.copy(
                estado = EstadosMercadillo.Estado.CANCELADO.codigo,
                version = original.version + 1,
                lastModified = 123456L,
                sincronizadoFirebase = false
            )
            assertEquals(esperado, dao.getMercadilloById(original.idMercadillo))
            assertEquals(otro, dao.getMercadilloById(otro.idMercadillo))
            val calendario = dao.getMercadillosByUserAndMes(original.userId, "%11-2026").first()
            assertEquals(setOf(esperado, otro), calendario.toSet())
        } finally {
            database.close()
        }
    }
}
