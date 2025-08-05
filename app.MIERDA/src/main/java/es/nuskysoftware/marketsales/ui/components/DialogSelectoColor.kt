package es.nuskysoftware.cajamercadillos.ui.components


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.rememberColorPickerController


/**
 * DialogSelectorColor:
 * Diálogo reutilizable con selector de color basado en Skydoves.
 * Incluye:
 *  - HsvColorPicker (selector principal)
 *  - Sliders de brillo y opacidad
 *  - Botones Aceptar / Cancelar
 */
@Composable
fun DialogSelectorColor(
    onColorElegido: (Color) -> Unit,
    onCancelar: () -> Unit
) {
    val controller = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = { onColorElegido(controller.selectedColor.value) }) {
                Text("Aceptar", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        title = { Text("Selecciona un color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Selector principal
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp),
                    controller = controller,
                    onColorChanged = {}
                )

                // === Vista previa del color seleccionado ===
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Color seleccionado")
                    // Cuadro de muestra en vivo
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        color = controller.selectedColor.value,
                        content = {}
                    )
                }

                // Sliders (opcional)
                Spacer(Modifier.height(8.dp))
                AlphaSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    controller = controller
                )
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    controller = controller
                )
            }
        }
    )
}
