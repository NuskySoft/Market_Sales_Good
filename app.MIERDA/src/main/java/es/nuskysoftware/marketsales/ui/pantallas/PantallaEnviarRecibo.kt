// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaEnviarRecibo.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Versión alineada con MainActivity:
 * - Muestra un resumen mínimo (Total + Método)
 * - Dos botones: Enviar (opcional) y Finalizar venta
 * La generación del recibo real y guardado se delega al ViewModel de Ventas
 * cuando volvamos con el savedStateHandle["finalizar_metodo"].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEnviarRecibo(
    totalFormateado: String,
    metodo: String,
    onBack: () -> Unit,
    onEnviar: () -> Unit,
    onFinalizarVenta: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recibo", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(totalFormateado, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(Modifier.height(8.dp))
            Text("Método de pago: ${metodo.uppercase()}", fontSize = 14.sp)

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEnviar,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Enviar") }

                Button(
                    onClick = onFinalizarVenta,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Finalizar venta") }
            }
        }
    }
}


