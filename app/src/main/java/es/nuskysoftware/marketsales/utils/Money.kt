package es.nuskysoftware.marketsales.utils

// Responsabilidad: Centraliza la precisión de importes: valida entradas y realiza cálculos mediante aritmética decimal.

import java.math.BigDecimal
import java.math.RoundingMode

/** Round at the monetary boundary; sums and products use decimal arithmetic. */
object Money {
    // Admite una parte decimal de hasta dos cifras, con coma o punto como separador.
    fun validInput(value: String): Boolean = value.matches(Regex("[0-9]*([.,][0-9]{0,2})?"))
    // Representa el importe con dos cifras decimales usando el criterio de redondeo definido aquí.
    fun decimal(value: Double): BigDecimal {
        require(value.isFinite()) { "Invalid amount" }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    }
    // Normaliza un importe antes de utilizarlo en una operación monetaria.
    fun round(value: Double): Double = decimal(value).toDouble()
    // Multiplica el precio normalizado por el número de unidades usando aritmética decimal.
    fun times(price: Double, quantity: Int): Double = decimal(price).multiply(BigDecimal.valueOf(quantity.toLong())).toDouble()
    // Acumula importes normalizados mediante BigDecimal, evitando sumar directamente valores binarios Double.
    fun sum(values: Iterable<Double>): Double = values.fold(BigDecimal.ZERO) { a, b -> a.add(decimal(b)) }.toDouble()
}
