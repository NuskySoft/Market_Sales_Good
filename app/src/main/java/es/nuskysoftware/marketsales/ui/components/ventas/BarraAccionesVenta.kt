// app/src/main/java/es/nuskysoftware/marketsales/ui/components/ventas/BarraAccionesVenta.kt
package es.nuskysoftware.marketsales.ui.components.ventas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun BarraAccionesVenta(
    ventasViewModel: VentasViewModel,
    totalFormateado: String,
    enabledRealizarCargo: Boolean,
    onRealizarCargo: () -> Unit,
    onAbrirCarrito: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onRealizarCargo,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            enabled = enabledRealizarCargo,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = StringResourceManager.getString("realizar_cargo", currentLanguage) + "  " + totalFormateado,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.background,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        BotonCarritoVentas(
            ventasViewModel = ventasViewModel,
            onClick = onAbrirCarrito
        )
    }
}

@Composable
fun BotonCarritoVentas(
    ventasViewModel: VentasViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by ventasViewModel.uiState.collectAsState()
    val badgeCount = remember(ui.lineasTicket) { ui.lineasTicket.sumOf { it.cantidad } }
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    BadgedBox(
        badge = {
            if (badgeCount > 0) {
                Badge { Text(badgeCount.toString()) }
            }
        }
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shopping_cart_24),
                contentDescription = StringResourceManager.getString("carrito", currentLanguage)
            )
        }
    }
}
