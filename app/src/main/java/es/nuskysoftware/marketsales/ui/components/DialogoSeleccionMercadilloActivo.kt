// app/src/main/java/es/nuskysoftware/marketsales/ui/components/DialogoSeleccionMercadilloActivo.kt
package es.nuskysoftware.marketsales.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.ConfigurationManager.moneda
import es.nuskysoftware.marketsales.utils.MonedaUtils

@Composable
fun DialogoSeleccionMercadilloActivo(
    mercadillosEnCurso: List<MercadilloEntity>,
    onMercadilloSeleccionado: (MercadilloEntity) -> Unit,
    onDismiss: () -> Unit,
    currentLanguage: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = StringResourceManager.getString(
                    "seleccionar_mercadillo_activo",
                    currentLanguage
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = StringResourceManager.getString(
                        "varios_mercadillos_en_curso",
                        currentLanguage
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                mercadillosEnCurso.forEach { m ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onMercadilloSeleccionado(m) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = m.lugar,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${m.organizador} • ${m.horaInicio} - ${m.horaFin}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )

                            if (m.saldoInicial != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val moneda by ConfigurationManager.moneda.collectAsState()
                                Text(
//                                    text = "💰 " +
//                                            StringResourceManager.getString("saldo_inicial", currentLanguage) +
//                                            ": €${String.format("%.2f", m.saldoInicial)}",
                                    text = "💰 ${StringResourceManager.getString("saldo_inicial", currentLanguage).ifBlank { "Saldo inicial" }}: " +
                                            MonedaUtils.formatearImporte(m.saldoInicial, moneda),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { /* la selección se realiza tocando una tarjeta */ },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = StringResourceManager.getString("cancelar", currentLanguage)
                )
            }
        }
    )
}
