package es.nuskysoftware.marketsales.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun CampoDescripcion(
    descripcion: String,
    onDescripcionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = descripcion,
        onValueChange = onDescripcionChange,
        label = { Text("Descripción *") },
        placeholder = { Text("Ej: Llavero Delfín Madera") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = descripcion.isBlank(),
        supportingText = {
            if (descripcion.isBlank()) {
                Text(
                    text = "La descripción es obligatoria",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}