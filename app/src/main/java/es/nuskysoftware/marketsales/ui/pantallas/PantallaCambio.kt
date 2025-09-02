// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaCambio.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.ui.composables.TecladoNumericoPago
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils
import es.nuskysoftware.marketsales.utils.StringResourceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCambio(
    totalFormateado: String,
    onBack: () -> Unit,
    onConfirmarCambio: (entregado: Double) -> Unit
) {
    val moneda by ConfigurationManager.moneda.collectAsState()
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    val total = remember(totalFormateado) {
        totalFormateado
            .replace(".", "")
            .replace(",", ".")
            .replace(Regex("[^0-9\\.]"), "")
            .toDoubleOrNull() ?: 0.0
    }

    var entregadoRaw by remember { mutableStateOf("0,00") }

    fun parseDouble(es: String): Double =
        es.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0

    val entregado = parseDouble(entregadoRaw)
    val cambio = (entregado - total).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("cambio_titulo", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Total
            Text(
                StringResourceManager.getString("total", currentLanguage),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                MonedaUtils.formatearImporte(total, moneda),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            // Entregado
            Text(
                StringResourceManager.getString("entregado", currentLanguage),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextoImporteGrande(importe = entregadoRaw)

            Spacer(Modifier.height(12.dp))

            // Teclado numérico
            TecladoNumericoPago(
                onDigitClick = { d ->
                    val solo = entregadoRaw.replace(",", "").replace(".", "")
                    val nuevo = (solo + d).takeLast(7).padStart(3, '0')
                    val ent = nuevo.dropLast(2).toLongOrNull()?.toString() ?: "0"
                    val dec = nuevo.takeLast(2)
                    entregadoRaw = "$ent,$dec"
                },
                onClearClick = {
                    val solo = entregadoRaw.replace(",", "").replace(".", "")
                    entregadoRaw = if (solo.length <= 1) "0,00" else {
                        val s = solo.dropLast(1).padStart(3, '0')
                        val ent = s.dropLast(2).toLongOrNull()?.toString() ?: "0"
                        val dec = s.takeLast(2)
                        "$ent,$dec"
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // Cambio
            Text(
                StringResourceManager.getString("cambio", currentLanguage),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                MonedaUtils.formatearImporte(cambio, moneda),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onConfirmarCambio(entregado) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    StringResourceManager.getString("confirmar", currentLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    AdsBottomBar()
}

@Composable
private fun TextoImporteGrande(importe: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = importe,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaCambio.kt
//package es.nuskysoftware.marketsales.ui.pantallas
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import es.nuskysoftware.marketsales.ui.composables.TecladoNumericoPago
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.MonedaUtils
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PantallaCambio(
//    totalFormateado: String,
//    onBack: () -> Unit,
//    onConfirmarCambio: (entregado: Double) -> Unit
//) {
//    val moneda by ConfigurationManager.moneda.collectAsState()
//    val total = remember(totalFormateado) {
//        // totalFormateado ya viene con coma y símbolo, extraemos double
//        totalFormateado
//            .replace(".", "")        // miles
//            .replace(",", ".")       // decimal
//            .replace(Regex("[^0-9\\.]"), "")
//            .toDoubleOrNull() ?: 0.0
//    }
//
//    var entregadoRaw by remember { mutableStateOf("0,00") }
//
//    fun parseDouble(es: String): Double =
//        es.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
//
//    val entregado = parseDouble(entregadoRaw)
//    val cambio = (entregado - total).coerceAtLeast(0.0)
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Cambio", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
//                navigationIcon = {
//                    TextButton(onClick = onBack) { Text("←") }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
//                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Total grande
//            Text("Total", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//            Text(
//                MonedaUtils.formatearImporte(total, moneda),
//                fontSize = 32.sp,
//                fontWeight = FontWeight.ExtraBold,
//                color = MaterialTheme.colorScheme.primary
//            )
//
//            Spacer(Modifier.height(12.dp))
//
//            // Entregado
//            Text("Entregado", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//            TextoImporteGrande(importe = entregadoRaw)
//
//            Spacer(Modifier.height(12.dp))
//
//            // Teclado numérico (mismo estilo)
//            TecladoNumericoPago(
//                onDigitClick = { d ->
//                    // Reutilizamos misma lógica que tu VM (2 decimales desplazando)
//                    val solo = entregadoRaw.replace(",", "").replace(".", "")
//                    val nuevo = (solo + d).takeLast(7).padStart(3, '0')
//                    val ent = nuevo.dropLast(2).toLongOrNull()?.toString() ?: "0"
//                    val dec = nuevo.takeLast(2)
//                    entregadoRaw = "$ent,$dec"
//                },
//                onClearClick = {
//                    val solo = entregadoRaw.replace(",", "").replace(".", "")
//                    entregadoRaw = if (solo.length <= 1) "0,00" else {
//                        val s = solo.dropLast(1).padStart(3, '0')
//                        val ent = s.dropLast(2).toLongOrNull()?.toString() ?: "0"
//                        val dec = s.takeLast(2)
//                        "$ent,$dec"
//                    }
//                }
//            )
//
//            Spacer(Modifier.height(16.dp))
//
//            // Cambio
//            Text("Cambio", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//            Text(
//                MonedaUtils.formatearImporte(cambio, moneda),
//                fontSize = 36.sp,
//                fontWeight = FontWeight.ExtraBold,
//                color = MaterialTheme.colorScheme.primary
//            )
//
//            Spacer(Modifier.height(20.dp))
//
//            Button(
//                onClick = { onConfirmarCambio(entregado) },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(64.dp),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Text("Confirmar", fontSize = 18.sp, fontWeight = FontWeight.Medium)
//            }
//        }
//    }
//}
//
//@Composable
//private fun TextoImporteGrande(importe: String) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Text(
//            text = importe,
//            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
//            textAlign = TextAlign.Center,
//            fontSize = 28.sp,
//            fontWeight = FontWeight.Bold
//        )
//    }
//}
//
//
