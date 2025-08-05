// app/src/main/java/es/nuskysoftware/marketsales/utils/MonedaUtils.kt
package es.nuskysoftware.marketsales.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utilidades de formato monetario basadas en ConfigurationManager.moneda.
 * Requisito: mostrar como "12,34€" (SIN espacio).
 */
object MonedaUtils {

    /** Obtiene el símbolo a partir de ConfigurationManager.moneda (ej. "€ Euro", "$ Dólar"). */
    fun obtenerSimboloMoneda(configMoneda: String): String {
        val raw = configMoneda.trim()
        if (raw.isEmpty()) return "€"
        val first = raw.first()
        // Si comienza con símbolo, lo usamos
        if (!first.isLetterOrDigit()) return first.toString()
        // Mapear por nombre
        val lower = raw.lowercase(Locale.ROOT)
        return when {
            "euro" in lower || "€" in lower -> "€"
            "dólar" in lower || "dolar" in lower || "$" in lower -> "$"
            "libra" in lower || "£" in lower -> "£"
            else -> "€"
        }
    }

    /**
     * Formatea un importe double a "12,34€" (coma decimal, 2 decimales, SIN espacio).
     */
    fun formatearImporte(valor: Double, configMoneda: String): String {
        val symbols = DecimalFormatSymbols(Locale("es", "ES")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
        val df = DecimalFormat("#,##0.00", symbols)
        val simbolo = obtenerSimboloMoneda(configMoneda)
        return df.format(valor) + simbolo
    }
}
