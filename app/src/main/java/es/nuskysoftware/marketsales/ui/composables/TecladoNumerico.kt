// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/TecladoNumerico.kt
package es.nuskysoftware.marketsales.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Teclado numérico con estilo:
 * - Texto negro (onSurface), fondo blanco (surface)
 * - Borde circular 2dp verde (primary)
 * - Botón borrar con borde rojo (error)
 */
@Composable
fun TecladoNumerico(
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onDoubleZeroClick: () -> Unit
) {
    val size = 64.dp
    val textSize = 22.sp

    @Composable
    fun Numero(texto: String, isBorrar: Boolean = false, onClick: () -> Unit) {
        val borderColor = if (isBorrar) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            shape = CircleShape,
            border = BorderStroke(2.dp, SolidColor(borderColor)),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isBorrar) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Borrar"
                )
            } else {
                Text(text = texto, fontSize = textSize, fontWeight = FontWeight.Medium)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Numero("1") { onDigitClick("1") }
            Spacer(Modifier.width(12.dp))
            Numero("2") { onDigitClick("2") }
            Spacer(Modifier.width(12.dp))
            Numero("3") { onDigitClick("3") }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Numero("4") { onDigitClick("4") }
            Spacer(Modifier.width(12.dp))
            Numero("5") { onDigitClick("5") }
            Spacer(Modifier.width(12.dp))
            Numero("6") { onDigitClick("6") }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Numero("7") { onDigitClick("7") }
            Spacer(Modifier.width(12.dp))
            Numero("8") { onDigitClick("8") }
            Spacer(Modifier.width(12.dp))
            Numero("9") { onDigitClick("9") }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Numero("00") { onDoubleZeroClick() }
            Spacer(Modifier.width(12.dp))
            Numero("0") { onDigitClick("0") }
            Spacer(Modifier.width(12.dp))
            Numero("⌫", isBorrar = true) { onClearClick() }
        }
    }
}

/**
 * Variante para PantallaCambio (mismo estilo).
 */
@Composable
fun TecladoNumericoPago(
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit
) {
    TecladoNumerico(
        onDigitClick = onDigitClick,
        onClearClick = onClearClick,
        onDoubleZeroClick = { onDigitClick("0"); onDigitClick("0") }
    )
}

