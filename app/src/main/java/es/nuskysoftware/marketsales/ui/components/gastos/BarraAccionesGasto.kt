// app/src/main/java/es/nuskysoftware/marketsales/ui/components/gastos/BarraAccionesGasto.kt
package es.nuskysoftware.marketsales.ui.components.gastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.R

@Composable
fun BarraAccionesGasto(
    enabledCargar: Boolean,
    numLineas: Int,
    onCargarGasto: () -> Unit,
    onAbrirCarrito: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botón Cargar Gasto
        Button(
            onClick = onCargarGasto,
            enabled = enabledCargar,
            modifier = Modifier
                .weight(1f)
                .size(height = 56.dp, width = 0.dp)
        ) {
            Text("Cargar Gasto", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        // Icono carrito con badge (= numLineas)
        BadgedBox(
            badge = {
                if (numLineas > 0) {
                    Badge { Text(numLineas.toString()) }
                }
            }
        ) {
            FilledTonalIconButton(
                onClick = onAbrirCarrito,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shopping_cart_24),
                    contentDescription = "Carrito",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/components/gastos/BarraAccionesGasto.kt
//package es.nuskysoftware.marketsales.ui.components.gastos
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import es.nuskysoftware.marketsales.R
//
//@Composable
//fun BarraAccionesGasto(
//    totalFormateado: String,
//    enabledCargar: Boolean,
//    numLineas: Int,
//    onCargarGasto: () -> Unit,
//    onAbrirCarrito: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Row(
//        modifier = modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Izquierda: total
//        Card(
//            modifier = Modifier
//                .weight(1f)
//                .height(56.dp),
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.surfaceVariant
//            )
//        ) {
//            Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = androidx.compose.ui.Alignment.CenterStart) {
//                Text(
//                    text = "Total gastos: $totalFormateado",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//        }
//
//        // Botón Cargar Gasto
//        Button(
//            onClick = onCargarGasto,
//            enabled = enabledCargar,
//            modifier = Modifier
//                .weight(1.2f)
//                .height(56.dp)
//        ) {
//            Text("Cargar Gasto", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
//        }
//
//        // Icono carrito con badge (= numLineas)
//        BadgedBox(
//            badge = {
//                if (numLineas > 0) {
//                    Badge { Text(numLineas.toString()) }
//                }
//            }
//        ) {
//            FilledTonalIconButton(
//                onClick = onAbrirCarrito,
//                modifier = Modifier.size(56.dp)
//            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_shopping_cart_24),
//                    contentDescription = "Carrito"
//                )
//            }
//        }
//    }
//}
