// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/resumen/PestanaResumenGastos.kt
package es.nuskysoftware.marketsales.ui.composables.resumen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun PestanaResumenGastos(
    mercadilloId: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val gastosDao = remember { db.lineasGastosDao() }

    val gastos by remember(mercadilloId) { gastosDao.observarGastosPorMercadillo(mercadilloId) }
        .collectAsState(initial = emptyList())
    val moneda by ConfigurationManager.moneda.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val totalGastos = remember(gastos) { gastos.sumOf { it.importe } }
    val totalFmt = MonedaUtils.formatearImporte(totalGastos, moneda)

    Column(Modifier.fillMaxSize()) {
        Text(
            text = StringResourceManager.getString("resumen_gastos", currentLanguage),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (gastos.isEmpty()) {
                item {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(gastos, key = { it.numeroLinea }) { g ->
                    GastoResumenRow(g, moneda)
                }
            }
        }

        Divider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                StringResourceManager.getString("total_gastos", currentLanguage),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = totalFmt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/* ======= UI helpers (gastos) ======= */

@Composable
private fun GastoResumenRow(g: LineaGastoEntity, moneda: String) {
    val importeFmt = remember(g, moneda) { MonedaUtils.formatearImporte(g.importe, moneda) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetodoPagoIcon(metodoPago = g.formaPago)
        Spacer(Modifier.width(10.dp))
        Text(g.descripcion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(importeFmt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetodoPagoIcon(metodoPago: String) {
    val emoji = when (metodoPago.lowercase()) {
        "efectivo" -> "💶"
        "bizum" -> "📲"
        "tarjeta" -> "💳"
        else -> "💰"
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = emoji,
            modifier = Modifier
                .size(28.dp)
                .wrapContentSize(Alignment.Center),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
