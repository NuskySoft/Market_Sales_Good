// app/src/main/java/es/nuskysoftware/marketsales/ui/components/BottomBarMercadillo.kt
package es.nuskysoftware.marketsales.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.utils.StringResourceManager

/**
 * BottomBar para operaciones con mercadillos en curso.
 * - Muestra, si aplica, un botón de ancho completo "Cambiar mercadillo seleccionado"
 *   por encima de los botones de Ventas / Gastos / Resumen.
 * - Ese botón solo aparece si hay un mercadillo seleccionado y existen >1 mercadillos en curso.
 */
@Composable
fun BottomBarMercadillo(
    mercadilloActivo: MercadilloEntity?,
    onVentasClick: () -> Unit,
    onGastosClick: () -> Unit,
    onResumenClick: () -> Unit,
    onCambiarMercadillo: () -> Unit,
    currentLanguage: String,
    mostrarBotonCambiar: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ⬆️ Botón de ancho completo (solo si hay selección y hay varios en curso)
            if (mercadilloActivo != null && mostrarBotonCambiar) {
                Button(
                    onClick = onCambiarMercadillo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = StringResourceManager.getString(
                            "cambiar_mercadillo_seleccionado",
                            currentLanguage
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Encabezado con info del mercadillo seleccionado (si lo hay)
            mercadilloActivo?.let { m ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = StringResourceManager.getString("mercadillo_activo", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = m.lugar,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Botones de acciones principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomBarButton(
                    icon = R.drawable.ic_ventas,
                    text = StringResourceManager.getString("ventas", currentLanguage),
                    onClick = onVentasClick,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                BottomBarButton(
                    icon = R.drawable.ic_gastos,
                    text = StringResourceManager.getString("gastos", currentLanguage),
                    onClick = onGastosClick,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                BottomBarButton(
                    icon = R.drawable.ic_resumen,
                    text = StringResourceManager.getString("resumen", currentLanguage),
                    onClick = onResumenClick,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Botón individual del BottomBar con icono y texto. */
@Composable
private fun BottomBarButton(
    icon: Int,
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(60.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}
