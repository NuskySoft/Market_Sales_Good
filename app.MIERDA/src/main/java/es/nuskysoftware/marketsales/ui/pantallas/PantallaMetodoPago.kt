
// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaMetodoPago.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

enum class MetodoPago { EFECTIVO, BIZUM, TARJETA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMetodoPago(
    totalFormateado: String,
    onMetodoSeleccionado: (MetodoPago) -> Unit,
    onBack: () -> Unit
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = StringResourceManager.getString("metodo_pago", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = StringResourceManager.getString("selecciona_metodo_pago", currentLanguage),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = StringResourceManager.getString("total", currentLanguage),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalFormateado,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            MetodoPagoButton("💶", StringResourceManager.getString("efectivo", currentLanguage),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            ) { onMetodoSeleccionado(MetodoPago.EFECTIVO) }

            Spacer(Modifier.height(12.dp))

            MetodoPagoButton("📲", StringResourceManager.getString("bizum", currentLanguage),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            ) { onMetodoSeleccionado(MetodoPago.BIZUM) }

            Spacer(Modifier.height(12.dp))

            MetodoPagoButton("💳", StringResourceManager.getString("tarjeta", currentLanguage),
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ) { onMetodoSeleccionado(MetodoPago.TARJETA) }
        }
    }
}

@Composable
private fun MetodoPagoButton(
    emoji: String,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) {
        Text("$emoji  $label", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}


