// app/src/main/java/es/nuskysoftware/marketsales/ui/componentsBarraAccionesVenta.kt
package es.nuskysoftware.marketsales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import es.nuskysoftware.marketsales.R

@Composable
fun BarraAccionesVenta(
    onRealizarCargo: () -> Unit,
    onAbrirCarrito: () -> Unit,
    badgeCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onRealizarCargo,
            modifier = Modifier.weight(1f)
        ) {
            //Icon(painter = painterResource(id = R.drawable.ic_checkout), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Realizar cargo")
        }

        Box(modifier = Modifier.weight(1f)) {
            Button(
                onClick = onAbrirCarrito,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_shopping_cart_24), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Carrito")
            }
            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(badgeCount.toString())
                }
            }
        }
    }
}
