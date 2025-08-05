package es.nuskysoftware.marketsales.ui.components.leyenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun LeyendaColoresDialog(
    onDismiss: () -> Unit,
    currentLanguage: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = StringResourceManager.getString("leyenda_estados", currentLanguage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadosMercadillo.obtenerTodosLosEstados().forEach { estado ->
                    Row {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(EstadosMercadillo.obtenerColor(estado), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = EstadosMercadillo.obtenerIcono(estado), fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = estado.descripcion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(StringResourceManager.getString("entendido", currentLanguage))
            }
        }
    )
}
