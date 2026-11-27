package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

/** Confirmación de cobro: el envío opcional se ofrece únicamente después de guardar la venta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEnviarRecibo(totalFormateado:String, metodo:String, onBack:()->Unit, onFinalizarVenta:()->Unit) {
    val language by ConfigurationManager.idioma.collectAsState()
    fun t(key:String)=StringResourceManager.getString(key,language)
    var pressed by remember { mutableStateOf(false) }
    Scaffold(topBar={TopAppBar(title={Text(t("recibo"),fontWeight=FontWeight.Bold)},
        navigationIcon={TextButton(enabled=!pressed,onClick=onBack){Text("←")}},
        colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.primary,
            titleContentColor=MaterialTheme.colorScheme.onPrimary,navigationIconContentColor=MaterialTheme.colorScheme.onPrimary))}){padding->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text(t("total"),style=MaterialTheme.typography.titleMedium)
            Text(totalFormateado,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
            Text(t("metodo_pago").replace("{metodo}",t(when(metodo.lowercase()){ "efectivo"->"metodo_efectivo";"bizum"->"metodo_bizum";else->"metodo_tarjeta"})))
            Button(enabled=!pressed,onClick={pressed=true;onFinalizarVenta()},modifier=Modifier.fillMaxWidth()){Text(t("finalizar_venta"))}
        }
    }
}
