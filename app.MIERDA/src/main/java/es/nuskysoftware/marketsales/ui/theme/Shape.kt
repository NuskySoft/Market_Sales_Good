package es.nuskysoftware.marketsales.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape.kt - Market Sales
 * Definición de formas para componentes Material3
 */

val Shapes = Shapes(
    // Formas pequeñas - para botones, chips, etc.
    small = RoundedCornerShape(4.dp),

    // Formas medianas - para cards, dialogs, etc.
    medium = RoundedCornerShape(8.dp),

    // Formas grandes - para bottom sheets, navigation drawers, etc.
    large = RoundedCornerShape(16.dp)
)

// Formas personalizadas adicionales
object CustomShapes {
    val extraSmall = RoundedCornerShape(2.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val circular = RoundedCornerShape(50)

    // Formas específicas para la app
    val cardShape = RoundedCornerShape(12.dp)
    val buttonShape = RoundedCornerShape(8.dp)
    val fabShape = RoundedCornerShape(16.dp)
    val dialogShape = RoundedCornerShape(20.dp)
}