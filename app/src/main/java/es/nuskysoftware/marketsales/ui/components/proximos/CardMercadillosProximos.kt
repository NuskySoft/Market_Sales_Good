// app/src/main/java/es/nuskysoftware/marketsales/ui/components/proximos/CardMercadillosProximos.kt
package es.nuskysoftware.marketsales.ui.components.proximos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.EstadosMercadillo
import es.nuskysoftware.marketsales.utils.MonedaUtils

import es.nuskysoftware.marketsales.utils.StringResourceManager
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CardMercadillosProximos(
    mercadillosProximos: List<MercadilloEntity>,
    onMercadilloClick: (MercadilloEntity) -> Unit
) {
    if (mercadillosProximos.isEmpty()) return

    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val listaOrdenada = mercadillosProximos.sortedWith(
        compareBy<MercadilloEntity>(
            { df.parse(it.fecha)?.time ?: Long.MAX_VALUE },
            { it.horaInicio }
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 🔑 Altura acotada: el scroll ocurre DENTRO del Card
            .heightIn(min = 160.dp, max = 340.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    StringResourceManager.getString("proximos_mercadillos", currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        listaOrdenada.size.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 🔑 Lista con scroll propio
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(listaOrdenada, key = { _, it -> it.idMercadillo }) { index, m ->
                    MercadilloProximoItem(m) { onMercadilloClick(m) }
                    if (index < listaOrdenada.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MercadilloProximoItem(
    mercadillo: MercadilloEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("📅  ${mercadillo.fecha} • ${mercadillo.horaInicio}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("📍  ${mercadillo.lugar}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text("👥  ${mercadillo.organizador}", fontSize = 12.sp)
            EstadosMercadillo.Estado.fromCodigo(mercadillo.estado)?.let { estado ->
                Spacer(Modifier.height(4.dp))
                Row {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(EstadosMercadillo.obtenerColor(estado))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        estado.descripcion,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            mercadillo.saldoInicial?.let {
                val moneda by ConfigurationManager.moneda.collectAsState()
                Spacer(Modifier.height(2.dp))
                Text(
                    //"💰  €${String.format("%.2f", it)}",
                    "💰  ${MonedaUtils.formatearImporte(it, moneda)}",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            "→",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
