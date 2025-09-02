// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaResumenVentas.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
internal fun LineaVentaResumenCard(
    linea: LineaVentaEntity,
    metodoPago: String,
    moneda: String
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val puLabel = StringResourceManager.getString("pu_label", currentLanguage)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEADING: icono método + chip cantidad
            Row(
                modifier = Modifier.width(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetodoPagoIcon(
                    metodoPago = metodoPago,
                    size = 20.dp
                )

                // Chip de cantidad
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = linea.cantidad.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Centro: descripción + PU
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = linea.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = puLabel + " " + MonedaUtils.formatearImporte(linea.precioUnitario, moneda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Derecha: total de línea
            Text(
                text = MonedaUtils.formatearImporte(linea.subtotal, moneda),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MetodoPagoIcon(
    metodoPago: String,
    size: Dp
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val contentDesc = when (metodoPago.uppercase()) {
        "EFECTIVO" -> StringResourceManager.getString("metodo_efectivo", currentLanguage)
        "BIZUM"    -> StringResourceManager.getString("metodo_bizum", currentLanguage)
        "TARJETA"  -> StringResourceManager.getString("metodo_tarjeta", currentLanguage)
        else       -> metodoPago
    }

    val drawableId = when (metodoPago.uppercase()) {
        "EFECTIVO" -> R.drawable.ic_cash
        "BIZUM"    -> R.drawable.ic_bizum
        "TARJETA"  -> R.drawable.ic_card
        else       -> 0
    }

    val tint: Color = when (metodoPago.uppercase()) {
        "EFECTIVO" -> MaterialTheme.colorScheme.primary
        "BIZUM"    -> MaterialTheme.colorScheme.tertiary
        "TARJETA"  -> MaterialTheme.colorScheme.secondary
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (drawableId != 0) {
        Icon(
            painter = painterResource(id = drawableId),
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier.size(size)
        )
    } else {
        val vec: ImageVector = when (metodoPago.uppercase()) {
            "EFECTIVO" -> Icons.Filled.AttachMoney
            "BIZUM"    -> Icons.Filled.Smartphone
            "TARJETA"  -> Icons.Filled.CreditCard
            else       -> Icons.Filled.AttachMoney
        }
        Icon(
            imageVector = vec,
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}
