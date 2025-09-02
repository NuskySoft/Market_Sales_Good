// app/src/main/java/es/nuskysoftware/marketsales/ui/componentsBarraAccionesVenta.kt
package es.nuskysoftware.marketsales.ui.components

import android.R.attr.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun BarraAccionesVenta(
    onRealizarCargo: () -> Unit,
    onAbrirCarrito: () -> Unit,
    badgeCount: Int,
    modifier: Modifier = Modifier
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

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
            Text(StringResourceManager.getString("realizar_cargo", currentLanguage))
        }

        Box(modifier = Modifier.weight(1f)) {
            Button(
                onClick = onAbrirCarrito,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_shopping_cart_24), contentDescription =  StringResourceManager.getString("carrito", currentLanguage))
                Spacer(Modifier.width(8.dp))
                Text(StringResourceManager.getString("carrito", currentLanguage))
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
