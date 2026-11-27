// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/CampoDescripcion.kt
package es.nuskysoftware.marketsales.ui.composables

// Responsabilidad: Campo reutilizable de descripción de conceptos introducidos por el usuario.

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
// Campo reutilizable de descripción de conceptos introducidos por el usuario.
fun CampoDescripcion(
    descripcion: String,
    onDescripcionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    OutlinedTextField(
        value = descripcion,
        onValueChange = onDescripcionChange,
        label = { Text(StringResourceManager.getString("descripcion_label", currentLanguage)) },
        placeholder = { Text(StringResourceManager.getString("descripcion_placeholder", currentLanguage)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = descripcion.isBlank(),
        supportingText = {
            if (descripcion.isBlank()) {
                Text(
                    text = StringResourceManager.getString("descripcion_obligatoria", currentLanguage),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
