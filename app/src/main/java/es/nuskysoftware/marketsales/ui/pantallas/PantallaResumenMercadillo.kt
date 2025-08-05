// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaResumenMercadillo.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.ui.composables.resumen.PestanaResumenGastos
import es.nuskysoftware.marketsales.ui.composables.resumen.PestanaResumenVentas
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaResumenVentas(
    mercadilloId: String,
    onBack: () -> Unit,
    mostrarAbono: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mercadilloVm: MercadilloViewModel = viewModel(factory = MercadilloViewModelFactory(context))
    LaunchedEffect(mercadilloId) { mercadilloVm.cargarMercadillo(mercadilloId) }
    val mercadillo by mercadilloVm.mercadilloParaEditar.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = mercadillo?.lugar ?: "Resumen", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_left), contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ventas") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Gastos") }
                )
            }

            when (selectedTab) {
                0 -> PestanaResumenVentas(mercadilloId = mercadilloId, mostrarAbono = mostrarAbono)
                1 -> PestanaResumenGastos(mercadilloId = mercadilloId)
            }
        }
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaResumenMercadillo.kt
//package es.nuskysoftware.marketsales.ui.pantallas
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import es.nuskysoftware.marketsales.R
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.LineaGastoEntity
//import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
//import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
//import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.MonedaUtils
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlin.math.abs
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaResumenVentas(
//    mercadilloId: String,
//    onBack: () -> Unit,
//    mostrarAbono: Boolean = true   // se desactiva cuando vienes de Arqueo
//) {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//    val snackbar = remember { SnackbarHostState() }
//
//    // Mercadillo (cabecera)
//    val mercadilloVm: MercadilloViewModel = viewModel(factory = MercadilloViewModelFactory(context))
//    LaunchedEffect(mercadilloId) { mercadilloVm.cargarMercadillo(mercadilloId) }
//    val mercadillo by mercadilloVm.mercadilloParaEditar.collectAsState()
//
//    // DAOs
//    val db = remember { AppDatabase.getDatabase(context) }
//    val lineasDao = remember { db.lineasVentaDao() }
//    val recibosDao = remember { db.recibosDao() }
//    val gastosDao = remember { db.lineasGastosDao() }
//
//    // Flujos ventas/recibos
//    val lineas by remember(mercadilloId) { lineasDao.obtenerLineasPorMercadillo(mercadilloId) }
//        .collectAsState(initial = emptyList())
//    val recibos by remember(mercadilloId) { recibosDao.obtenerRecibosPorMercadillo(mercadilloId) }
//        .collectAsState(initial = emptyList())
//
//    // Mapa idRecibo -> método de pago
//    val metodoPorRecibo = remember(recibos) { recibos.associate { it.idRecibo to it.metodoPago } }
//
//    val moneda by ConfigurationManager.moneda.collectAsState()
//
//    // Orden estable por idLinea
//    val lineasOrdenadas by remember(lineas) { mutableStateOf(lineas.sortedBy { it.idLinea }) }
//
//    // Conjunto de originales que YA tienen un abono creado
//    val originalesAbonadas: Set<String> = remember(lineas) {
//        lineas.mapNotNull { it.idLineaOriginalAbonada }.toSet()
//    }
//
//    val totalMercadillo = remember(lineasOrdenadas) { lineasOrdenadas.sumOf { it.subtotal } }
//    val totalFmt = MonedaUtils.formatearImporte(totalMercadillo, moneda)
//
//    // Estado para el diálogo de confirmación
//    var lineaParaConfirmar by remember { mutableStateOf<LineaVentaEntity?>(null) }
//
//    // ====== Gastos para el “Resumen de Gastos” ======
//    val gastos by remember(mercadilloId) {
//        gastosDao.observarGastosPorMercadillo(mercadilloId)
//    }.collectAsState(initial = emptyList())
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(text = mercadillo?.lugar ?: "", fontWeight = FontWeight.Bold) },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(painter = painterResource(id = R.drawable.ic_arrow_left), contentDescription = "Atrás")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
//                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            )
//        },
//        snackbarHost = { SnackbarHost(snackbar) }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//            // ===== Título ventas =====
//            Text(
//                text = "Resumen de ventas",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
//            )
//
//            // ===== Lista de ventas =====
//            LazyColumn(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth(),
//                contentPadding = PaddingValues(12.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(lineasOrdenadas, key = { it.idLinea }) { linea ->
//                    val metodo = metodoPorRecibo[linea.idRecibo] ?: ""
//                    val yaEsAbono = linea.idLineaOriginalAbonada != null
//                    val yaFueAbonada = originalesAbonadas.contains(linea.idLinea)
//
//                    LineaResumenCard(
//                        linea = linea,
//                        moneda = moneda,
//                        metodoPago = metodo,
//                        mostrarBotonAbono = mostrarAbono && !yaEsAbono && !yaFueAbonada,
//                        onAbonarClick = { lineaParaConfirmar = linea }
//                    )
//                }
//            }
//
//            Divider()
//
//            // ===== Total de ventas =====
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 12.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("Total de ventas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
//                Text(text = totalFmt, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
//            }
//
//            // ====== RESUMEN DE GASTOS ======
//            Divider()
//            Text(
//                text = "Resumen de Gastos",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
//            )
//
//            // 🔽 Lista de gastos con scroll propio
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(240.dp), // altura fija para garantizar scroll interno
//                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                if (gastos.isEmpty()) {
//                    item {
//                        Text(
//                            text = "—",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 8.dp),
//                            textAlign = TextAlign.Center
//                        )
//                    }
//                } else {
//                    items(gastos, key = { it.numeroLinea }) { g ->
//                        GastoResumenRow(g, moneda)
//                    }
//                }
//            }
//        }
//    }
//
//    // ===== AlertDialog de confirmación de abono =====
//    val lineaConfirm = lineaParaConfirmar
//    if (lineaConfirm != null) {
//        val totalLineaFmt = MonedaUtils.formatearImporte(lineaConfirm.subtotal, moneda)
//        AlertDialog(
//            onDismissRequest = { lineaParaConfirmar = null },
//            title = { Text("Confirmar abono") },
//            text = {
//                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                    Text("¿Seguro que quieres abonar esta línea?")
//                    Text("• ${lineaConfirm.descripcion}")
//                    Text("• Cantidad: ${lineaConfirm.cantidad}")
//                    Text("• Total línea: $totalLineaFmt")
//                }
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        val linea = lineaConfirm
//                        lineaParaConfirmar = null
//                        scope.launch(Dispatchers.IO) {
//                            try {
//                                val maxId = lineasDao.obtenerMaxIdLineaPorMercadillo(linea.idMercadillo)
//                                val nuevoId = siguienteIdLinea(maxId)
//                                val nextNumero = (lineas.maxOfOrNull { it.numeroLinea } ?: 0) + 1
//
//                                val abono = LineaVentaEntity(
//                                    idLinea = nuevoId,
//                                    idRecibo = linea.idRecibo,
//                                    idMercadillo = linea.idMercadillo,
//                                    idUsuario = linea.idUsuario,
//                                    numeroLinea = nextNumero,
//                                    tipoLinea = linea.tipoLinea,
//                                    descripcion = linea.descripcion,
//                                    idProducto = linea.idProducto,
//                                    cantidad = -abs(linea.cantidad),
//                                    precioUnitario = linea.precioUnitario,
//                                    subtotal = -abs(linea.subtotal),
//                                    idLineaOriginalAbonada = linea.idLinea
//                                )
//                                lineasDao.insertarLinea(abono)
//                                launch { snackbar.showSnackbar("Abono creado") }
//                            } catch (e: Exception) {
//                                launch { snackbar.showSnackbar("Error creando abono: ${e.message}") }
//                            }
//                        }
//                    }
//                ) { Text("Sí, abonar") }
//            },
//            dismissButton = {
//                TextButton(onClick = { lineaParaConfirmar = null }) { Text("Cancelar") }
//            }
//        )
//    }
//}
//
//@Composable
//private fun GastoResumenRow(g: LineaGastoEntity, moneda: String) {
//    val importeFmt = remember(g, moneda) { MonedaUtils.formatearImporte(g.importe, moneda) }
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        MetodoPagoIcon(metodoPago = g.formaPago)           // ← icono método de pago (💶/📲/💳)
//        Spacer(Modifier.width(10.dp))
//        Text(g.descripcion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
//        Text(importeFmt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
//    }
//}
//
//@Composable
//private fun LineaResumenCard(
//    linea: LineaVentaEntity,
//    moneda: String,
//    metodoPago: String,
//    mostrarBotonAbono: Boolean,
//    onAbonarClick: () -> Unit
//) {
//    val precioUnitFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.precioUnitario, moneda) }
//    val totalLineaFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.subtotal, moneda) }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp, vertical = 10.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            MetodoPagoIcon(metodoPago = metodoPago)
//            Spacer(Modifier.width(10.dp))
//            Text(text = linea.cantidad.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(text = linea.descripcion, fontWeight = FontWeight.Medium)
//                Text(
//                    text = "PU: $precioUnitFmt",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            Column(horizontalAlignment = Alignment.End) {
//                Text(text = totalLineaFmt, fontWeight = FontWeight.SemiBold)
//                if (mostrarBotonAbono) {
//                    Spacer(Modifier.height(6.dp))
//                    OutlinedButton(onClick = onAbonarClick, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
//                        Text("Abonar")
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun MetodoPagoIcon(metodoPago: String) {
//    // Iconos estilo original: emoji dentro de círculo
//    val emoji = when (metodoPago.lowercase()) {
//        "efectivo" -> "💶"
//        "bizum" -> "📲"
//        "tarjeta" -> "💳"
//        else -> "💰"
//    }
//    Surface(
//        shape = CircleShape,
//        color = MaterialTheme.colorScheme.primaryContainer,
//        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
//    ) {
//        Text(
//            text = emoji,
//            modifier = Modifier
//                .size(28.dp)
//                .wrapContentSize(Alignment.Center),
//            style = MaterialTheme.typography.bodyLarge
//        )
//    }
//}
//
///* ===== Helpers ===== */
//
//private fun siguienteIdLinea(maxId: String?): String {
//    if (maxId.isNullOrBlank()) return "0001"
//    val len = maxId.length
//    val n = maxId.toIntOrNull() ?: return "0001"
//    val next = n + 1
//    return next.toString().padStart(len, '0')
//}


//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaResumenVentas.kt
//package es.nuskysoftware.marketsales.ui.pantallas
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import es.nuskysoftware.marketsales.R
//import es.nuskysoftware.marketsales.data.local.database.AppDatabase
//import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
//import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModel
//import es.nuskysoftware.marketsales.ui.viewmodel.MercadilloViewModelFactory
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.MonedaUtils
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlin.math.abs
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaResumenVentas(
//    mercadilloId: String,
//    onBack: () -> Unit,
//    mostrarAbono: Boolean = true   // se desactiva cuando vienes de Arqueo
//) {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//    val snackbar = remember { SnackbarHostState() }
//
//    // Mercadillo (cabecera)
//    val mercadilloVm: MercadilloViewModel = viewModel(factory = MercadilloViewModelFactory(context))
//    LaunchedEffect(mercadilloId) { mercadilloVm.cargarMercadillo(mercadilloId) }
//    val mercadillo by mercadilloVm.mercadilloParaEditar.collectAsState()
//
//    // DAOs
//    val db = remember { AppDatabase.getDatabase(context) }
//    val lineasDao = remember { db.lineasVentaDao() }
//    val recibosDao = remember { db.recibosDao() }
//
//    // Flujos
//    val lineas by remember(mercadilloId) { lineasDao.obtenerLineasPorMercadillo(mercadilloId) }
//        .collectAsState(initial = emptyList())
//    val recibos by remember(mercadilloId) { recibosDao.obtenerRecibosPorMercadillo(mercadilloId) }
//        .collectAsState(initial = emptyList())
//
//    // Mapa idRecibo -> método
//    val metodoPorRecibo = remember(recibos) { recibos.associate { it.idRecibo to it.metodoPago } }
//
//    val moneda by ConfigurationManager.moneda.collectAsState()
//
//    // Orden estable por idLinea
//    val lineasOrdenadas by remember(lineas) { mutableStateOf(lineas.sortedBy { it.idLinea }) }
//
//    // Conjunto de originales que YA tienen un abono creado
//    val originalesAbonadas: Set<String> = remember(lineas) {
//        lineas.mapNotNull { it.idLineaOriginalAbonada }.toSet()
//    }
//
//    val totalMercadillo = remember(lineasOrdenadas) { lineasOrdenadas.sumOf { it.subtotal } }
//    val totalFmt = MonedaUtils.formatearImporte(totalMercadillo, moneda)
//
//    // Estado para el diálogo de confirmación
//    var lineaParaConfirmar by remember { mutableStateOf<LineaVentaEntity?>(null) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(text = mercadillo?.lugar ?: "", fontWeight = FontWeight.Bold) },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(painter = painterResource(id = R.drawable.ic_arrow_left), contentDescription = "Atrás")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
//                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            )
//        },
//        snackbarHost = { SnackbarHost(snackbar) }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//            Text(
//                text = "Resumen de ventas",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
//            )
//
//            LazyColumn(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth(),
//                contentPadding = PaddingValues(12.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(lineasOrdenadas, key = { it.idLinea }) { linea ->
//                    val metodo = metodoPorRecibo[linea.idRecibo] ?: ""
//                    val yaEsAbono = linea.idLineaOriginalAbonada != null
//                    val yaFueAbonada = originalesAbonadas.contains(linea.idLinea)
//
//                    LineaResumenCard(
//                        linea = linea,
//                        moneda = moneda,
//                        metodoPago = metodo,
//                        mostrarBotonAbono = mostrarAbono && !yaEsAbono && !yaFueAbonada,
//                        onAbonarClick = { lineaParaConfirmar = linea }
//                    )
//                }
//            }
//
//            Divider()
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 12.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("Total de ventas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
//                Text(text = totalFmt, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
//            }
//        }
//    }
//
//    // ===== AlertDialog de confirmación de abono =====
//    val lineaConfirm = lineaParaConfirmar
//    if (lineaConfirm != null) {
//        val totalLineaFmt = MonedaUtils.formatearImporte(lineaConfirm.subtotal, moneda)
//        AlertDialog(
//            onDismissRequest = { lineaParaConfirmar = null },
//            title = { Text("Confirmar abono") },
//            text = {
//                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                    Text("¿Seguro que quieres abonar esta línea?")
//                    Text("• ${lineaConfirm.descripcion}")
//                    Text("• Cantidad: ${lineaConfirm.cantidad}")
//                    Text("• Total línea: $totalLineaFmt")
//                }
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        // Ejecutar abono
//                        val linea = lineaConfirm
//                        lineaParaConfirmar = null
//                        scope.launch(Dispatchers.IO) {
//                            try {
//                                val maxId = lineasDao.obtenerMaxIdLineaPorMercadillo(linea.idMercadillo)
//                                val nuevoId = siguienteIdLinea(maxId)
//                                val nextNumero = (lineas.maxOfOrNull { it.numeroLinea } ?: 0) + 1
//
//                                val abono = LineaVentaEntity(
//                                    idLinea = nuevoId,
//                                    idRecibo = linea.idRecibo,
//                                    idMercadillo = linea.idMercadillo,
//                                    idUsuario = linea.idUsuario,
//                                    numeroLinea = nextNumero,
//                                    tipoLinea = linea.tipoLinea,
//                                    descripcion = linea.descripcion,
//                                    idProducto = linea.idProducto,
//                                    cantidad = -abs(linea.cantidad),
//                                    precioUnitario = linea.precioUnitario,
//                                    subtotal = -abs(linea.subtotal),
//                                    idLineaOriginalAbonada = linea.idLinea
//                                )
//                                lineasDao.insertarLinea(abono)
//                                launch { snackbar.showSnackbar("Abono creado") }
//                            } catch (e: Exception) {
//                                launch { snackbar.showSnackbar("Error creando abono: ${e.message}") }
//                            }
//                        }
//                    }
//                ) { Text("Sí, abonar") }
//            },
//            dismissButton = {
//                TextButton(onClick = { lineaParaConfirmar = null }) { Text("Cancelar") }
//            }
//        )
//    }
//}
//
//@Composable
//private fun LineaResumenCard(
//    linea: LineaVentaEntity,
//    moneda: String,
//    metodoPago: String,
//    mostrarBotonAbono: Boolean,
//    onAbonarClick: () -> Unit
//) {
//    val precioUnitFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.precioUnitario, moneda) }
//    val totalLineaFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.subtotal, moneda) }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp, vertical = 10.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            MetodoPagoBadge(metodoPago = metodoPago)
//            Spacer(Modifier.width(10.dp))
//            Text(text = linea.cantidad.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(text = linea.descripcion, fontWeight = FontWeight.Medium)
//                Text(
//                    text = "PU: $precioUnitFmt",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            Column(horizontalAlignment = Alignment.End) {
//                Text(text = totalLineaFmt, fontWeight = FontWeight.SemiBold)
//                if (mostrarBotonAbono) {
//                    Spacer(Modifier.height(6.dp))
//                    OutlinedButton(
//                        onClick = onAbonarClick,
//                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
//                    ) {
//                        Text("Abonar")
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun MetodoPagoBadge(metodoPago: String) {
//    val emoji = when (metodoPago.lowercase()) {
//        "efectivo" -> "💶"
//        "bizum" -> "📲"
//        "tarjeta" -> "💳"
//        else -> "💰"
//    }
//    Surface(
//        shape = CircleShape,
//        color = MaterialTheme.colorScheme.primaryContainer,
//        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
//    ) {
//        Text(
//            text = emoji,
//            modifier = Modifier
//                .size(28.dp)
//                .wrapContentSize(Alignment.Center),
//            textAlign = TextAlign.Center,
//            fontSize = 16.sp
//        )
//    }
//}
//
///* ===== Helpers ===== */
//
//private fun siguienteIdLinea(maxId: String?): String {
//    if (maxId.isNullOrBlank()) return "0001"
//    val len = maxId.length
//    val n = maxId.toIntOrNull() ?: return "0001"
//    val next = n + 1
//    return next.toString().padStart(len, '0')
//}
//
