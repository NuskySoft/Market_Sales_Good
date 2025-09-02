// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaResultadoMercadillo.kt
package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaResultadoMercadillo(
    navController: NavController,
    mercadilloId: String
) {
    val context = LocalContext.current
    val repo = remember { MercadilloRepository(context) }
    val vm: ArqueoViewModel = viewModel(factory = ArqueoViewModelFactory(context, repo))
    val ui by vm.ui.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    // Gastos por método (reales)
    val db = remember { AppDatabase.getDatabase(context) }
    var gastosEf by remember { mutableStateOf(0.0) }
    var gastosBi by remember { mutableStateOf(0.0) }
    var gastosTa by remember { mutableStateOf(0.0) }

    LaunchedEffect(mercadilloId) {
        vm.cargar(mercadilloId)
        withContext(Dispatchers.IO) {
            gastosEf = db.lineasGastosDao().getTotalGastosPorMetodo(mercadilloId, "efectivo")
            gastosBi = db.lineasGastosDao().getTotalGastosPorMetodo(mercadilloId, "bizum")
            gastosTa = db.lineasGastosDao().getTotalGastosPorMetodo(mercadilloId, "tarjeta")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResourceManager.getString("resultado_mercadillo", currentLanguage)) },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
                        )
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading -> Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { Text(ui.error!!, color = MaterialTheme.colorScheme.error) }

            ui.mercadillo != null -> {
                val m = ui.mercadillo!!

                val ventasTotal = m.ventasEfectivo + m.ventasBizum + m.ventasTarjeta
                val gastosMetodos = gastosEf + gastosBi + gastosTa
                val gastosTotal = gastosMetodos + m.importeSuscripcion // suscripción = gasto
                val resultado = ventasTotal - gastosTotal // sin saldo inicial

                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                            Text(
                                StringResourceManager.getString("ventas_por_metodo", currentLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            KeyValueLine(
                                StringResourceManager.getString("efectivo", currentLanguage),
                                vm.fmtMoneda(m.ventasEfectivo)
                            )
                            KeyValueLine(
                                StringResourceManager.getString("bizum", currentLanguage),
                                vm.fmtMoneda(m.ventasBizum)
                            )
                            KeyValueLine(
                                StringResourceManager.getString("tarjeta", currentLanguage),
                                vm.fmtMoneda(m.ventasTarjeta)
                            )
                            KeyValueLine(
                                StringResourceManager.getString("total_ventas", currentLanguage),
                                vm.fmtMoneda(ventasTotal),
                                bold = true
                            )

                            Divider()

                            Text(
                                StringResourceManager.getString("gastos_por_metodo", currentLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            KeyValueLine(
                                StringResourceManager.getString("efectivo", currentLanguage),
                                vm.fmtMoneda(gastosEf)
                            )
                            KeyValueLine(
                                StringResourceManager.getString("bizum", currentLanguage),
                                vm.fmtMoneda(gastosBi)
                            )
                            KeyValueLine(
                                StringResourceManager.getString("tarjeta", currentLanguage),
                                vm.fmtMoneda(gastosTa)
                            )
                            KeyValueLine(
                                StringResourceManager.getString("total_gastos_metodos", currentLanguage),
                                vm.fmtMoneda(gastosMetodos)
                            )

                            Divider()
                            KeyValueLine(
                                StringResourceManager.getString("suscripcion", currentLanguage),
                                vm.fmtMoneda(m.importeSuscripcion)
                            )

                            Divider()
                            KeyValueLine(
                                StringResourceManager.getString("resultado_mercadillo", currentLanguage),
                                vm.fmtMoneda(resultado),
                                bold = true
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ===== Helper local ===== */

@Composable
private fun KeyValueLine(label: String, value: String, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else null
        )
    }
}
