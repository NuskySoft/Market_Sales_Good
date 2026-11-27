// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/resumen/PestanaResumenVentas.kt
package es.nuskysoftware.marketsales.ui.composables.resumen

// Responsabilidad: Presenta las ventas registradas, sus métodos de pago y las acciones relacionadas con abonos.

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
import es.nuskysoftware.marketsales.data.local.entity.LineaVentaEntity
import es.nuskysoftware.marketsales.data.repository.ArticuloRepository
import es.nuskysoftware.marketsales.data.repository.TipoLinea
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
// Presenta las líneas de ventas y el detalle de pago y abono del mercadillo consultado.
fun PestanaResumenVentas(
    mercadilloId: String,
    mostrarAbono: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getDatabase(context) }
    val lineasDao = remember { db.lineasVentaDao() }
    val recibosDao = remember { db.recibosDao() }
    val articuloRepository = remember { ArticuloRepository(context) }

    val lineas by remember(mercadilloId) { lineasDao.obtenerLineasPorMercadillo(mercadilloId) }
        .collectAsState(initial = emptyList())
    val recibos by remember(mercadilloId) { recibosDao.obtenerRecibosPorMercadillo(mercadilloId) }
        .collectAsState(initial = emptyList())

    val metodoPorRecibo = remember(recibos) { recibos.associate { it.idRecibo to it.metodoPago } }
    val moneda by ConfigurationManager.moneda.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    // 1) Separar originales y abonos
    val originalesOrdenadas = remember(lineas) {
        lineas.filter { it.idLineaOriginalAbonada == null }.sortedBy { line -> recibos.firstOrNull { it.idRecibo == line.idRecibo }?.fechaHora ?: line.lastModified }
    }
    val abonosPorOriginal = remember(lineas) {
        lineas.filter { it.idLineaOriginalAbonada != null }
            .groupBy { it.idLineaOriginalAbonada!! } // clave = id de la línea original
            .mapValues { (_, lista) -> lista.sortedBy { it.lastModified } }
    }

    // Para ocultar botón "Abonar" si ya existe al menos un abono
    val originalesConAbono: Set<String> = remember(abonosPorOriginal) { abonosPorOriginal.keys }

    // 2) Construir el orden final: original, luego sus abonos
    val ordenFinal: List<LineaVentaEntity> = remember(originalesOrdenadas, abonosPorOriginal) {
        buildList {
            originalesOrdenadas.forEach { orig ->
                add(orig)
                abonosPorOriginal[orig.idLinea]?.let { addAll(it) }
            }
        }
    }

    val totalMercadillo = remember(lineas) { es.nuskysoftware.marketsales.utils.Money.sum(lineas.map { it.subtotal }) }
    val totalFmt = MonedaUtils.formatearImporte(totalMercadillo, moneda)

    // Diálogo de confirmación
    var lineaParaConfirmar by remember { mutableStateOf<LineaVentaEntity?>(null) }
    var metodoDevolucion by remember { mutableStateOf<String?>(null) }
    var refundBusy by remember { mutableStateOf(false) }
    var refundError by remember { mutableStateOf<String?>(null) }
    var selectedReceipt by remember { mutableStateOf<String?>(null) }
    val receiptOwner by ConfigurationManager.usuarioLogueado.collectAsState()
    val premium by ConfigurationManager.esPremium.collectAsState()
    val documents by remember(receiptOwner) { db.receiptDocumentDao().ids(receiptOwner.orEmpty()) }.collectAsState(initial=emptyList())
    val external = remember(receiptOwner) { es.nuskysoftware.marketsales.receipts.ReceiptSettings(context,receiptOwner.orEmpty()).folder }
    val salesRepository = remember { es.nuskysoftware.marketsales.data.repository.VentasRepository(context) }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = StringResourceManager.getString("resumen_ventas", currentLanguage),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ordenFinal, key = { it.idLinea }) { linea ->
                val metodo = metodoPorRecibo[linea.idRecibo] ?: ""
                val esAbono = linea.idLineaOriginalAbonada != null

                if (esAbono) {
                    // Línea de ABONO (debajo de la original)
                    AbonoResumenCard(
                        linea = linea,
                        moneda = moneda,
                        metodoPago = metodo
                    )
                } else {
                    // Línea original
                    val puedeAbonar = mostrarAbono && !originalesConAbono.contains(linea.idLinea)
                    LineaResumenCard(
                        linea = linea,
                        moneda = moneda,
                        metodoPago = metodo,
                        mostrarBotonAbono = puedeAbonar,
                        onAbonarClick = {
                            if (es.nuskysoftware.marketsales.utils.DemoAccess.isDemo()) es.nuskysoftware.marketsales.utils.DemoAccess.show("demo_abonos_registrados")
                            else { metodoDevolucion = null; lineaParaConfirmar = linea }
                        }
                    )
                }
                // Una acción por recibo, no por cada concepto del mismo ticket.
                if (premium && (linea.idRecibo in documents || (external != null && recibos.any { it.idRecibo == linea.idRecibo && it.pdfOriginal })) && ordenFinal.firstOrNull { it.idRecibo == linea.idRecibo }?.idLinea == linea.idLinea) {
                    TextButton(onClick = { selectedReceipt = linea.idRecibo }) {
                        Text(if (currentLanguage == "en") "Receipt PDF" else "Recibo PDF")
                    }
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
                StringResourceManager.getString("total_ventas", currentLanguage),
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

    selectedReceipt?.let { es.nuskysoftware.marketsales.ui.recibos.ReceiptActions(it, onDismiss = { selectedReceipt = null }) }

    // Confirmar creación de abono
    val lineaConfirm = lineaParaConfirmar
    if (lineaConfirm != null) {
        val totalLineaFmt = MonedaUtils.formatearImporte(lineaConfirm.subtotal, moneda)
        AlertDialog(
            onDismissRequest = { lineaParaConfirmar = null },
            title = { Text(StringResourceManager.getString("confirmar_abono", currentLanguage)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(StringResourceManager.getString("confirmar_abono_pregunta", currentLanguage))
                    Text(StringResourceManager.getString("refund_payment_method", currentLanguage))
                    listOf("EFECTIVO", "BIZUM", "TARJETA").forEach { method ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = metodoDevolucion == method, onClick = { metodoDevolucion = method })
                            Text(StringResourceManager.getString(method.lowercase(), currentLanguage))
                        }
                    }
                    refundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text("• ${lineaConfirm.descripcion}")
                    Text("• " + StringResourceManager.getString("cantidad", currentLanguage) + ": ${lineaConfirm.cantidad}")
                    Text("• " + StringResourceManager.getString("total_linea", currentLanguage) + ": $totalLineaFmt")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = metodoDevolucion != null && !refundBusy,
                    onClick = {
                        val l = lineaConfirm
                        val method = metodoDevolucion ?: return@TextButton
                        refundBusy = true
                        scope.launch {
                            val result = salesRepository.crearAbono(l, l.cantidad, l.idMercadillo, method)
                            refundBusy = false
                            result.onSuccess { id ->
                                    lineaParaConfirmar = null; refundError = null
                                    if (premium && es.nuskysoftware.marketsales.receipts.ReceiptSettings(context,l.idUsuario).ask) selectedReceipt = id
                                }
                                .onFailure { refundError = it.message ?: StringResourceManager.getString("local_save_failed", currentLanguage) }
                        }
                    }
                ) { Text(StringResourceManager.getString("si_abonar", currentLanguage)) }
            },
            dismissButton = {
                TextButton(onClick = { lineaParaConfirmar = null }) {
                    Text(StringResourceManager.getString("cancelar", currentLanguage))
                }
            }
        )
    }
}

/* ======= UI helpers ======= */

@Composable
private fun LineaResumenCard(
    linea: LineaVentaEntity,
    moneda: String,
    metodoPago: String,
    mostrarBotonAbono: Boolean,
    onAbonarClick: () -> Unit
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val precioUnitFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.precioUnitario, moneda) }
    val totalLineaFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.subtotal, moneda) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetodoPagoIcon(metodoPago = metodoPago)
            Spacer(Modifier.width(10.dp))
            Text(text = linea.cantidad.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = linea.descripcion, fontWeight = FontWeight.Medium)
                Text(
                    text = StringResourceManager.getString("precio_unitario", currentLanguage) + " " + precioUnitFmt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = totalLineaFmt, fontWeight = FontWeight.SemiBold)
                if (mostrarBotonAbono) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onAbonarClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(StringResourceManager.getString("abonar", currentLanguage))
                    }
                }
            }
        }
    }
}

@Composable
private fun AbonoResumenCard(
    linea: LineaVentaEntity,
    moneda: String,
    metodoPago: String
) {
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val precioUnitFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.precioUnitario, moneda) }
    val totalLineaFmt = remember(linea, moneda) { MonedaUtils.formatearImporte(linea.subtotal, moneda) }

    // Mismo layout, pero con etiqueta "ABONO" y sin botón
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp), // leve indentación para diferenciar
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetodoPagoIcon(metodoPago = metodoPago)
            Spacer(Modifier.width(10.dp))

            // Cantidad (negativa) + etiqueta ABONO
            Column(
                modifier = Modifier.width(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = linea.cantidad.toString(), fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = linea.descripcion, fontWeight = FontWeight.Medium)
                Text(
                    text = StringResourceManager.getString("precio_unitario", currentLanguage) + " " + precioUnitFmt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text(StringResourceManager.getString("abono_chip", currentLanguage)) }
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = totalLineaFmt, fontWeight = FontWeight.SemiBold)
            }
        }
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

private fun siguienteIdLinea(maxId: String?): String {
    if (maxId.isNullOrBlank()) return "0001"
    val len = maxId.length
    val n = maxId.toIntOrNull() ?: return "0001"
    val next = n + 1
    return next.toString().padStart(len, '0')
}
