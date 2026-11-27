package es.nuskysoftware.marketsales.data

import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import es.nuskysoftware.marketsales.data.repository.GastoSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GastoSyncTest {
    @Test
    fun claveCompuestaYCamposCompletos() {
        val gasto = LineaGastoEntity(
            idMercadillo = "a", idUsuario = "owner", numeroLinea = "0001",
            descripcion = "Gasto", importe = 12.34, fechaHora = 100L,
            formaPago = "bizum", descontarDeCaja = false, lastModified = 123L
        )
        assertEquals("a-0001", GastoSync.documentId(gasto))
        assertNotEquals(GastoSync.documentId(gasto), GastoSync.documentId(gasto.copy(idMercadillo = "b")))
        val fields = GastoSync.fields(gasto)
        assertEquals("owner", fields["idUsuario"])
        assertEquals("0001", fields["numeroLinea"])
        assertEquals("bizum", fields["formaPago"])
        assertEquals(false, fields["descontarDeCaja"])
        assertEquals(100L, fields["fechaHora"])
        assertEquals(123L, fields["lastModified"])
    }
}
