// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaArqueo.kt
package es.nuskysoftware.marketsales.ui.pantallas.arqueo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.safePopBackStack
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
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResourceManager.getString("arqueo", currentLanguage)) },
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
                        Text(
                            StringResourceManager.getString("cargando_datos_mercadillo", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Column
            }

            // ===== Ficha =====
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }

                    Text(data.organizador, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        label = StringResourceManager.getString("importe_suscripcion", currentLanguage),
                        value = vm.fmtMoneda(data.importeSuscripcion)
                    )
                    InfoRow(
                        label = StringResourceManager.getString("saldo_inicial", currentLanguage),
                        value = vm.fmtMoneda(data.saldoInicial)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EstadoChip(data.estado, currentLanguage)
                        OutlinedButton(
                            onClick = { navController.navigate("resumen/$mercadilloId") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                StringResourceManager.getString("resumen_mercadillo", currentLanguage),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // ===== Ventas (rejilla 2x2) =====
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        StringResourceManager.getString("ventas", currentLanguage),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Fila 1: Bizum / Tarjeta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = StringResourceManager.getString("bizum", currentLanguage),
                            valueText = vm.fmtMoneda(data.ventasBizum),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = StringResourceManager.getString("tarjeta", currentLanguage),
                            valueText = vm.fmtMoneda(data.ventasTarjeta),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Fila 2: Efectivo / Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = StringResourceManager.getString("efectivo", currentLanguage),
                            valueText = vm.fmtMoneda(data.ventasEfectivo),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = StringResourceManager.getString("total", currentLanguage),
                            valueText = vm.fmtMoneda(data.totalVentas),
                            emphasized = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ===== Acciones =====
            val puedeArqueo = data.estado == 4
            val esPremium = remember { ConfigurationManager.isPremium() }
            val puedeAsignar = data.estado == 5 || (!esPremium && data.estado == 6)

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            StringResourceManager.getString("acciones", currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        Button(
                            onClick = { navController.navigate("arqueo/caja/$mercadilloId") },
                            enabled = puedeArqueo,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(StringResourceManager.getString("realizar_arqueo", currentLanguage)) }
                    }
                    item {
                        Button(
                            onClick = { navController.navigate("arqueo/resultado/$mercadilloId") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(StringResourceManager.getString("resultado_mercadillo", currentLanguage)) }
                    }
                    item {
                        Button(
                            onClick = { navController.navigate("arqueo/asignar-saldo/$mercadilloId") },
                            enabled = puedeAsignar,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(StringResourceManager.getString("asignar_saldo", currentLanguage)) }
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val uid = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId()
                                    if (!uid.isNullOrBlank()) {
                                        es.nuskysoftware.marketsales.data.repository.MercadilloRepository(context)
                                            .actualizarEstadosAutomaticos(uid)
                                    }
                                    navController.safePopBackStack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(StringResourceManager.getString("cerrar", currentLanguage)) }
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
private fun MetricCard(
    title: String,
    valueText: String,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val colors = if (emphasized)
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    else CardDefaults.cardColors()

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
                valueText,
                style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EstadoChip(estado: Int, currentLanguage: String) {
    val (bg, txt, labelKey) = when (estado) {
        4 -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "estado_pendiente_arqueo"
        )
        5 -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "estado_pendiente_asignar_saldo"
        )
        6 -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "estado_cerrado_completo"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "guion_largo"
        )
    }
    val label = StringResourceManager.getString(labelKey, currentLanguage)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = txt, style = MaterialTheme.typography.labelMedium)
    }
}


//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/arqueo/PantallaArqueo.kt
//package es.nuskysoftware.marketsales.ui.pantallas.arqueo
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.data.repository.MercadilloRepository
//import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModel
//import es.nuskysoftware.marketsales.ui.viewmodel.ArqueoViewModelFactory
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.safePopBackStack
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaArqueo(
//    navController: NavController,
//    mercadilloId: String
//) {
//    val context = LocalContext.current
//    val vm: ArqueoViewModel = viewModel(
//        factory = ArqueoViewModelFactory(
//            context = context,
//            repository = MercadilloRepository(context)
//        )
//    )
//
//    LaunchedEffect(mercadilloId) { vm.cargar(mercadilloId) }
//    val ui by vm.ui.collectAsState()
//    val scope = rememberCoroutineScope()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Arqueo") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.safePopBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
//                    }
//                }
//            )
//        }
//    ) { pad ->
//        if (ui.loading) {
//            LinearProgressIndicator(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(pad)
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
//            )
//        }
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(pad)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            ui.error?.let { msg ->
//                AssistChip(onClick = {}, enabled = false, label = { Text(msg) })
//            }
//
//            val data = ui.mercadillo ?: run {
//                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
//                    Column(Modifier.padding(16.dp)) {
//                        Text("Cargando datos de mercadillo…", style = MaterialTheme.typography.bodyMedium)
//                    }
//                }
//                return@Column
//            }
//
//            // ===== Ficha =====
//            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
//                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            "${data.fecha} · ${data.lugar}",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                    }
//
//                    Text(data.organizador, style = MaterialTheme.typography.bodyMedium)
//                    Spacer(Modifier.height(8.dp))
//                    InfoRow("Suscripción", vm.fmtMoneda(data.importeSuscripcion))
//                    InfoRow("Saldo inicial", vm.fmtMoneda(data.saldoInicial))
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        EstadoChip(data.estado)
//                        OutlinedButton(
//                            onClick = { navController.navigate("resumen/$mercadilloId") },
//                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
//                        ) {
//                            Text("Resumen Mercadillo", style = MaterialTheme.typography.labelLarge)
//                        }
//                    }
//                }
//            }
//
//            // ===== Ventas (rejilla 2x2) =====
//            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
//                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
//                    Text("Ventas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
//
//                    // Fila 1: Bizum / Tarjeta
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        MetricCard(
//                            title = "Bizum",
//                            valueText = vm.fmtMoneda(data.ventasBizum),
//                            modifier = Modifier.weight(1f)
//                        )
//                        MetricCard(
//                            title = "Tarjeta",
//                            valueText = vm.fmtMoneda(data.ventasTarjeta),
//                            modifier = Modifier.weight(1f)
//                        )
//                    }
//
//                    // Fila 2: Efectivo / Total (Total destacado)
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        MetricCard(
//                            title = "Efectivo",
//                            valueText = vm.fmtMoneda(data.ventasEfectivo),
//                            modifier = Modifier.weight(1f)
//                        )
//                        MetricCard(
//                            title = "Total",
//                            valueText = vm.fmtMoneda(data.totalVentas),
//                            emphasized = true,
//                            modifier = Modifier.weight(1f)
//                        )
//                    }
//                }
//            }
//
//            // ===== Acciones =====
//            val puedeArqueo = data.estado == 4
////            val puedeAsignar = data.estado == 5
//            val esPremium = remember { ConfigurationManager.isPremium() }
//            val puedeAsignar = data.estado == 5 || (!esPremium && data.estado == 6)
//
//            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
//                LazyColumn(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalArrangement = Arrangement.spacedBy(12.dp),
//                    contentPadding = PaddingValues(16.dp)
//                ) {
//                    item {
//                        Text(
//                            "Acciones",
//                            style = MaterialTheme.typography.titleSmall,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                    }
//                    item {
//                        Button(
//                            onClick = { navController.navigate("arqueo/caja/$mercadilloId") },
//                            enabled = puedeArqueo,
//                            modifier = Modifier.fillMaxWidth()
//                        ) { Text("Realizar arqueo") }
//                    }
//                    item {
//                        Button(
//                            onClick = { navController.navigate("arqueo/resultado/$mercadilloId") },
//                            modifier = Modifier.fillMaxWidth()
//                        ) { Text("Resultado del mercadillo") }
//                    }
//                    item {
//                        Button(
//                            onClick = { navController.navigate("arqueo/asignar-saldo/$mercadilloId") },
//                            enabled = puedeAsignar,
//                            modifier = Modifier.fillMaxWidth()
//                        ) { Text("Asignar saldo") }
//                    }
//                    item {
//                        OutlinedButton(
//                            onClick = {
//                                scope.launch {
//                                    val uid = es.nuskysoftware.marketsales.utils.ConfigurationManager.getCurrentUserId()
//                                    if (!uid.isNullOrBlank()) {
//                                        es.nuskysoftware.marketsales.data.repository.MercadilloRepository(context)
//                                            .actualizarEstadosAutomaticos(uid)
//                                    }
//                                    navController.safePopBackStack()
//                                }
//                            },
//                            modifier = Modifier.fillMaxWidth()
//                        ) { Text("Cerrar") }
//                    }
//                }
//            }
//        }
//    }
//}
//
///* ===== helpers de UI ===== */
//
//@Composable
//private fun InfoRow(label: String, value: String) {
//    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
//        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
//    }
//}
//
//@Composable
//private fun MetricCard(
//    title: String,
//    valueText: String,
//    emphasized: Boolean = false,
//    modifier: Modifier = Modifier
//) {
//    val shape = RoundedCornerShape(14.dp)
//    val colors = if (emphasized)
//        CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer,
//            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
//        )
//    else CardDefaults.cardColors()
//
//    Card(modifier = modifier, shape = shape, colors = colors) {
//        Column(
//            Modifier.padding(12.dp),
//            verticalArrangement = Arrangement.spacedBy(6.dp),
//            horizontalAlignment = Alignment.Start
//        ) {
//            Text(
//                title,
//                style = if (emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
//                color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
//                else MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                valueText,
//                style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.SemiBold
//            )
//        }
//    }
//}
//
//@Composable
//private fun EstadoChip(estado: Int) {
//    val (bg, txt, label) = when (estado) {
//        4 -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "Pendiente de arqueo")
//        5 -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Pendiente de asignar saldo")
//        6 -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Cerrado")
//        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "—")
//    }
//    Box(
//        modifier = Modifier
//            .clip(RoundedCornerShape(12.dp))
//            .background(bg)
//            .padding(horizontal = 10.dp, vertical = 6.dp)
//    ) {
//        Text(label, color = txt, style = MaterialTheme.typography.labelMedium)
//    }
//}
//
