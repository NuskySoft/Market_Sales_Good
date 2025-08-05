// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/gastos/PestanaGastosManual.kt
package es.nuskysoftware.marketsales.ui.composables.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
import es.nuskysoftware.marketsales.ui.components.gastos.BarraAccionesGasto
import es.nuskysoftware.marketsales.ui.composables.CampoDescripcion
import es.nuskysoftware.marketsales.ui.composables.CampoImporte
import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils

@Composable
fun PestanaGastosManual(
    gastosViewModel: GastosViewModel,
    mercadilloActivo: MercadilloEntity,
    onAbrirCarrito: () -> Unit,
    onCargarGasto: (totalFormateado: String) -> Unit
) {
    val ui by gastosViewModel.uiState.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Importe (solo lectura + grande)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CampoImporte(importe = ui.importeActual, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(8.dp))

        // Descripción
        CampoDescripcion(
            descripcion = ui.descripcionActual,
            onDescripcionChange = gastosViewModel::actualizarDescripcion,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Teclado numérico
        es.nuskysoftware.marketsales.ui.composables.TecladoNumerico(
            onDigitClick = gastosViewModel::onDigitoPresionado,
            onClearClick = gastosViewModel::onBorrarDigito,
            onDoubleZeroClick = gastosViewModel::onDobleDecimalPresionado
        )

        Spacer(Modifier.height(20.dp))

        // Botón Añadir gasto
        Button(
            onClick = { gastosViewModel.añadirGastoManual() },
            enabled = ui.descripcionActual.isNotBlank() && gastosViewModel.obtenerImporteComoDouble() > 0.0,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir gasto", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Acciones inferiores (sin tarjeta de “Total gastos”)
        val totalFmt = MonedaUtils.formatearImporte(ui.totalCarrito, moneda)
        BarraAccionesGasto(
            enabledCargar = ui.lineasCarrito.isNotEmpty() && ui.totalCarrito > 0.0,
            numLineas = ui.lineasCarrito.size,
            onCargarGasto = { if (ui.totalCarrito > 0.0) onCargarGasto(totalFmt) },
            onAbrirCarrito = onAbrirCarrito,
            modifier = Modifier.fillMaxWidth()
        )
    }
}



//// app/src/main/java/es/nuskysoftware/marketsales/ui/composables/gastos/PestanaGastosManual.kt
//package es.nuskysoftware.marketsales.ui.composables.gastos
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
//import es.nuskysoftware.marketsales.ui.components.gastos.BarraAccionesGasto
//import es.nuskysoftware.marketsales.ui.composables.CampoDescripcion
//import es.nuskysoftware.marketsales.ui.composables.CampoImporte
//import es.nuskysoftware.marketsales.ui.viewmodel.GastosViewModel
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.MonedaUtils
//
//@Composable
//fun PestanaGastosManual(
//    gastosViewModel: GastosViewModel,
//    mercadilloActivo: MercadilloEntity,
//    onAbrirCarrito: () -> Unit,
//    onCargarGasto: (totalFormateado: String) -> Unit
//) {
//    val ui by gastosViewModel.uiState.collectAsState()
//    val moneda by ConfigurationManager.moneda.collectAsState()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(8.dp)
//    ) {
//        // Importe grande (readonly, alimentado por teclado)
//        Box(
//            modifier = Modifier.fillMaxWidth(),
//            contentAlignment = Alignment.Center
//        ) {
//            CampoImporte(importe = ui.importeActual, modifier = Modifier.fillMaxWidth())
//        }
//
//        Spacer(Modifier.height(8.dp))
//
//        // Descripción
//        CampoDescripcion(
//            descripcion = ui.descripcionActual,
//            onDescripcionChange = gastosViewModel::actualizarDescripcion,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(Modifier.height(8.dp))
//
//        // Teclado numérico de la app (dos decimales)
//        es.nuskysoftware.marketsales.ui.composables.TecladoNumerico(
//            onDigitClick = gastosViewModel::onDigitoPresionado,
//            onClearClick = gastosViewModel::onBorrarDigito,
//            onDoubleZeroClick = gastosViewModel::onDobleDecimalPresionado
//        )
//
//        Spacer(Modifier.height(20.dp))
//
//        // Botón Añadir gasto
//        Button(
//            onClick = { gastosViewModel.añadirGastoManual() },
//            enabled = ui.descripcionActual.isNotBlank() && gastosViewModel.obtenerImporteComoDouble() > 0.0,
//            modifier = Modifier.fillMaxWidth().height(56.dp)
//        ) {
//            Icon(Icons.Default.Add, contentDescription = null)
//            Spacer(Modifier.width(8.dp))
//            Text("Añadir gasto", fontSize = 16.sp)
//        }
//
//        Spacer(Modifier.height(16.dp))
//
//        // Barra inferior: total + cargar + carrito
//        val totalFmt = MonedaUtils.formatearImporte(ui.totalCarrito, moneda)
//        BarraAccionesGasto(
//            totalFormateado = totalFmt,
//            enabledCargar = ui.lineasCarrito.isNotEmpty() && ui.totalCarrito > 0.0,
//            numLineas = ui.lineasCarrito.size,
//            onCargarGasto = { if (ui.totalCarrito > 0.0) onCargarGasto(totalFmt) },
//            onAbrirCarrito = onAbrirCarrito,
//            modifier = Modifier.fillMaxWidth()
//        )
//    }
//}
//
