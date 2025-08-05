package es.nuskysoftware.marketsales.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity

import es.nuskysoftware.marketsales.ui.components.ventas.BarraAccionesVenta
import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.MonedaUtils

@Composable
fun PestanaVentaManual(
    navController: NavController,
    ventasViewModel: VentasViewModel,
    mercadilloActivo: MercadilloEntity,
    onRealizarCargo: (totalFormateado: String) -> Unit
) {
    val uiState by ventasViewModel.uiState.collectAsState()
    val moneda by ConfigurationManager.moneda.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        // Importe grande
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            CampoImporte(
                importe = uiState.importeActual,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(6.dp))

        // Descripción
        CampoDescripcion(
            descripcion = uiState.descripcionActual,
            onDescripcionChange = ventasViewModel::actualizarDescripcion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(6.dp))

        // Teclado numérico
        TecladoNumerico(
            onDigitClick = ventasViewModel::onDigitoPresionado,
            onClearClick = ventasViewModel::onBorrarDigito,
            onDoubleZeroClick = ventasViewModel::onDobleDecimalPresionado
        )

        Spacer(Modifier.height(24.dp))

        // Botón "Añadir Venta" (antes: "Añadir más ventas")
        Button(
            onClick = {
                if (uiState.descripcionActual.isNotBlank() &&
                    ventasViewModel.obtenerImporteComoDouble() > 0
                ) {
                    ventasViewModel.añadirLineaManual()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState.descripcionActual.isNotBlank() &&
                    ventasViewModel.obtenerImporteComoDouble() > 0,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir")
            Spacer(Modifier.width(8.dp))
            Text("Añadir Venta", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(16.dp))

        // Botones inferiores (Realizar cargo + Carrito)
        val totalFmt = MonedaUtils.formatearImporte(uiState.totalTicket, moneda)
        BarraAccionesVenta(
            ventasViewModel = ventasViewModel,
            totalFormateado = totalFmt,
            enabledRealizarCargo = uiState.lineasTicket.isNotEmpty() && uiState.totalTicket > 0,
            onRealizarCargo = { if (uiState.totalTicket > 0) onRealizarCargo(totalFmt) },
            onAbrirCarrito = { navController.navigate("carrito/${mercadilloActivo.idMercadillo}") }
        )

        Spacer(Modifier.height(16.dp))
    }
}




//package es.nuskysoftware.marketsales.ui.composables
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import es.nuskysoftware.marketsales.data.local.entity.MercadilloEntity
//import es.nuskysoftware.marketsales.ui.components.ventas.BarraAccionesVenta
//import es.nuskysoftware.marketsales.ui.viewmodel.VentasViewModel
//import es.nuskysoftware.marketsales.utils.ConfigurationManager
//import es.nuskysoftware.marketsales.utils.MonedaUtils
//
//@Composable
//fun PestanaVentaManual(
//    ventasViewModel: VentasViewModel,
//    navController: NavController,
//    mercadilloActivo: MercadilloEntity,
//    onRealizarCargo: (totalFormateado: String) -> Unit
//) {
//    val uiState by ventasViewModel.uiState.collectAsState()
//    val moneda by ConfigurationManager.moneda.collectAsState()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(6.dp)
//    ) {
//        // Importe grande
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(2.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            CampoImporte(
//                importe = uiState.importeActual,
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        Spacer(Modifier.height(6.dp))
//
//        // Descripción
//        CampoDescripcion(
//            descripcion = uiState.descripcionActual,
//            onDescripcionChange = ventasViewModel::actualizarDescripcion,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(Modifier.height(6.dp))
//
//        // Teclado numérico
//        TecladoNumerico(
//            onDigitClick = ventasViewModel::onDigitoPresionado,
//            onClearClick = ventasViewModel::onBorrarDigito,
//            onDoubleZeroClick = ventasViewModel::onDobleDecimalPresionado
//        )
//
//        Spacer(Modifier.height(24.dp))
//
//        // Añadir línea manual
//        Button(
//            onClick = {
//                if (uiState.descripcionActual.isNotBlank() &&
//                    ventasViewModel.obtenerImporteComoDouble() > 0
//                ) {
//                    ventasViewModel.añadirLineaManual()
//                }
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(56.dp),
//            enabled = uiState.descripcionActual.isNotBlank() &&
//                    ventasViewModel.obtenerImporteComoDouble() > 0,
//            shape = RoundedCornerShape(12.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = MaterialTheme.colorScheme.primary
//            )
//        ) {
//            Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir")
//            Spacer(Modifier.width(8.dp))
//            Text("Añadir más ventas", fontSize = 16.sp, fontWeight = FontWeight.Medium)
//        }
//
//        Spacer(Modifier.height(16.dp))
//
//        // Botones inferiores (extraídos al módulo externo)
//        val totalFmt = MonedaUtils.formatearImporte(uiState.totalTicket, moneda)
//        BarraAccionesVenta(
//            ventasViewModel = ventasViewModel,
//            totalFormateado = totalFmt,
//            enabledRealizarCargo = uiState.lineasTicket.isNotEmpty() && uiState.totalTicket > 0,
//            onRealizarCargo = { if (uiState.totalTicket > 0) onRealizarCargo(totalFmt) },
//            onAbrirCarrito = { navController.navigate("carrito/${mercadilloActivo.idMercadillo}") }
//        )
//
//        Spacer(Modifier.height(16.dp))
//    }
//}
