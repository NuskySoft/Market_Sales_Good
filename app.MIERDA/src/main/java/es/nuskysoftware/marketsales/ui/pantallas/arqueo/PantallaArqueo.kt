// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaArqueo.kt
/*
 Módulo: Pantalla de hub de Arqueo (estados 4/5/6).
 - Muestra ficha (fecha, lugar, organizador, suscripción, saldo inicial) y métricas de ventas.
 - Botón "Listado ventas" dentro de la ficha (arriba derecha) → navega a resumen/{mercadilloId}.
 - Ventas en rejilla 2x2: (Bizum, Tarjeta) / (Efectivo, Total) con Total destacado.
*/

package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaArqueo(
    navController: NavController,
    mercadilloId: String
) {
    val context = LocalContext.current
    val vm: ArqueoViewModel = viewModel(
        factory = ArqueoViewModelFactory(
            context = context,
            repository = MercadilloRepository(context)
        )
    )

    LaunchedEffect(mercadilloId) { vm.cargar(mercadilloId) }
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arqueo") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("←") }
                }
            )
        }
    ) { pad ->
        if (ui.loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(pad)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ui.error?.let { msg ->
                AssistChip(onClick = {}, enabled = false, label = { Text(msg) })
            }

            val data = ui.mercadillo ?: run {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cargando datos de mercadillo…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                return@Column
            }

            // ===== Ficha =====
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Título + botón "Listado ventas"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${data.fecha} · ${data.lugar}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
//                        OutlinedButton(
//                            onClick = { navController.navigate("resumen/$mercadilloId") },
//                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
//                        ) {
//                            Text("Listado ventas", style = MaterialTheme.typography.labelLarge)
//                        }
                    }

                    Text(data.organizador, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Suscripción", formateaEuros(data.importeSuscripcion))
                    InfoRow("Saldo inicial", formateaEuros(data.saldoInicial))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chip de estado con color de fondo del estado
                        EstadoChip(data.estado)
                        OutlinedButton(
                            onClick = { navController.navigate("resumen/$mercadilloId") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Listado ventas", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ===== Ventas (rejilla 2x2) =====
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ventas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    // Fila 1: Bizum / Tarjeta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Bizum",
                            value = data.ventasBizum,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Tarjeta",
                            value = data.ventasTarjeta,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Fila 2: Efectivo / Total (Total destacado)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Efectivo",
                            value = data.ventasEfectivo,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Total",
                            value = data.totalVentas,
                            emphasized = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
// ===== Acciones =====
            val puedeArqueo = data.estado == 4
            val puedeAsignar = data.estado == 5

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            "Acciones",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        Button(
                            onClick = { navController.navigate("arqueo/caja/$mercadilloId") },
                            enabled = puedeArqueo,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Realizar arqueo") }
                    }
                    item {
                        Button(
                            onClick = { navController.navigate("arqueo/resultado/$mercadilloId") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Resultado del mercadillo") }
                    }
                    item {
                        Button(
                            onClick = { navController.navigate("arqueo/asignar-saldo/$mercadilloId") },
                            enabled = puedeAsignar,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Asignar saldo") }
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    // ✅ revisar estados al cerrar PantallaArqueo
                                    val uid = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId()
                                    if (!uid.isNullOrBlank()) {
                                        es.nuskysoftware.marketsales.data.repository.MercadilloRepository(context)
                                            .actualizarEstadosAutomaticos(uid)
                                    }
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
//                            onClick = { navController.popBackStack() },
//                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cerrar") }
                    }
                }
            }
        }
    }
}

/* ===== helpers de UI ===== */

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetricCard(title: String, value: Double, emphasized: Boolean = false, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val colors = if (emphasized)
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    else
        CardDefaults.cardColors()

    Card(modifier = modifier, shape = shape, colors = colors) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                title,
                style = if (emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formateaEuros(value),
                style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EstadoChip(estado: Int) {
    val (bg, txt, label) = when (estado) {
        4 -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "Pendiente de arqueo")
        5 -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Pendiente de asignar saldo")
        6 -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Cerrado")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "—")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = txt, style = MaterialTheme.typography.labelMedium)
    }
}

private fun formateaEuros(v: Double) = "€ " + String.format("%,.2f", v)

