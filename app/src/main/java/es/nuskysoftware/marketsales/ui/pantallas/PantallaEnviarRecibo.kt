// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaEnviarRecibo.kt
package es.nuskysoftware.marketsales.ui.pantallas

// Responsabilidad: Presenta las opciones de envío de recibos al usuario.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.nuskysoftware.marketsales.ads.AdsBottomBar
import es.nuskysoftware.marketsales.utils.ValidationUtils
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

enum class TipoEnvio { CORREO, WHATSAPP }
/**
 * Versión alineada con MainActivity:
 * - Muestra un resumen mínimo (Total + Método)
 * - Dos botones: Enviar (opcional) y Finalizar venta
 * La generación del recibo real y guardado se delega al ViewModel de Ventas
 * cuando volvamos con el savedStateHandle["finalizar_metodo"].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Presenta las opciones de envío de recibos al usuario.
fun PantallaEnviarRecibo(
    totalFormateado: String,
    metodo: String,
    onBack: () -> Unit,
    onEnviar: (TipoEnvio, String) -> Unit,
    onFinalizarVenta: () -> Unit
) {
    val currentLanguage = ConfigurationManager.idioma.collectAsState().value

    var tipo by remember { mutableStateOf(TipoEnvio.CORREO) }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf(ValidationUtils.prefijoPorDefecto()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val emailFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Ejecuta este efecto al entrar en composición o cuando cambian sus claves (tipo).
    LaunchedEffect(tipo) {
        if (tipo == TipoEnvio.WHATSAPP) {
            phoneFocus.requestFocus()
            keyboard?.show()
        } else {
            emailFocus.requestFocus()
            keyboard?.show()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResourceManager.getString("recibo", currentLanguage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←") // símbolo, no se internacionaliza
                    }
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
            Text(
                StringResourceManager.getString("total", currentLanguage),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(totalFormateado, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(8.dp))
            errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

            Spacer(Modifier.height(8.dp))
            Text(
                StringResourceManager.getString("metodo_pago", currentLanguage).replace("{metodo}", StringResourceManager.getString(when (metodo.lowercase()) { "efectivo" -> "metodo_efectivo"; "bizum" -> "metodo_bizum"; "tarjeta" -> "metodo_tarjeta"; else -> "metodo_pago" }, currentLanguage)),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {
                        keyboard?.hide()
                        onFinalizarVenta()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) { Text(StringResourceManager.getString("finalizar_venta", currentLanguage)) }
            }
        }
    }
    AdsBottomBar()
}

