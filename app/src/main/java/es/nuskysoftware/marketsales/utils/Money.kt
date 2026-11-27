package es.nuskysoftware.marketsales.utils

import java.math.BigDecimal
import java.math.RoundingMode

/** Round at the monetary boundary; sums and products use decimal arithmetic. */
object Money {
    fun validInput(value: String): Boolean = value.matches(Regex("[0-9]*([.,][0-9]{0,2})?"))
    fun decimal(value: Double): BigDecimal {
        require(value.isFinite()) { "Invalid amount" }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    }
    fun round(value: Double): Double = decimal(value).toDouble()
    fun times(price: Double, quantity: Int): Double = decimal(price).multiply(BigDecimal.valueOf(quantity.toLong())).toDouble()
    fun sum(values: Iterable<Double>): Double = values.fold(BigDecimal.ZERO) { a, b -> a.add(decimal(b)) }.toDouble()
}
