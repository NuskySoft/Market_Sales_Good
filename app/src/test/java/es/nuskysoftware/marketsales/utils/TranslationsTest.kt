package es.nuskysoftware.marketsales.utils

// Finalidad de las pruebas: Comprueba la coherencia entre los catálogos de traducción utilizados por la aplicación.

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TranslationsTest {
    @Test
    fun utilidadesPerfilYEmpresaTienenTraduccionEnAmbosIdiomas() {
        val keys = listOf(
            "utilidades", "utilidades_premium_msg", "saldos_pendientes", "datos_empresa",
            "perfil", "opciones_de_privacidad", "nif_dni", "nombre_empresa", "razon_social",
            "direccion", "poblacion", "codigo_postal", "provincia", "pais",
            "crear_nueva_categoria", "selecciona_metodo_pago", "SI", "NO"
        )
        for (key in keys) {
            for (language in listOf("es", "en")) {
                val value = StringResourceManager.getString(key, language)
                assertFalse("$language:$key no puede estar vacío", value.isBlank())
                assertFalse("$language:$key no puede mostrar la clave", value == key)
            }
        }
    }

    @Test
    fun perfilYUtilidadesMuestranElIdiomaSolicitado() {
        assertEquals("Opciones de privacidad", StringResourceManager.getString("opciones_de_privacidad", "es"))
        assertEquals("Privacy options", StringResourceManager.getString("opciones_de_privacidad", "en"))
        assertEquals("Datos de empresa", StringResourceManager.getString("datos_empresa", "es"))
        assertEquals("Business details", StringResourceManager.getString("datos_empresa", "en"))
    }
}
