package es.nuskysoftware.marketsales.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.MercadillosFilters
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun DialogoSeleccionMercadillo(
    mercadillos: List<MercadilloEntity>,
    currentUserId: String?,                 // 👈 para evitar mostrar mercadillos de otros usuarios
    onMercadilloSeleccionado: (MercadilloEntity) -> Unit,
    onDismiss: () -> Unit,
    currentLanguage: String
) {
    val lista = MercadillosFilters.soloDelUsuario(mercadillos, currentUserId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Seleccionar mercadillo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(lista) { mercadillo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMercadilloSeleccionado(mercadillo) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(mercadillo.lugar, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("${mercadillo.organizador} • ${mercadillo.horaInicio} - ${mercadillo.horaFin}")
                            Spacer(Modifier.height(4.dp))
                            EstadosMercadillo.Estado.fromCodigo(mercadillo.estado)?.let { estado ->
                                Row {
                                    Text(text = EstadosMercadillo.obtenerIcono(estado), fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = estado.descripcion, color = EstadosMercadillo.obtenerColor(estado))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(StringResourceManager.getString("cancelar", currentLanguage)) }
        }
    )
}
