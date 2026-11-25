package es.nuskysoftware.marketsales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.DemoAccess
import es.nuskysoftware.marketsales.utils.StringResourceManager

/** Fixed outside every screen's scrolling content, including authentication screens. */
@Composable
fun DemoChrome(nav: NavController, ready: Boolean = true, content: @Composable () -> Unit) {
    val owner by ConfigurationManager.usuarioLogueado.collectAsState()
    val language by ConfigurationManager.idioma.collectAsState()
    val notice by DemoAccess.notice.collectAsState()
    val demo = DemoAccess.isDemo()
    fun t(key: String) = StringResourceManager.getString(key, language)
    Column(Modifier.fillMaxSize()) {
        if (demo) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(t("version_demo"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = ready, onClick = { nav.navigate("registro") { launchSingleTop = true } }) { Text(t("demo_crear_cuenta")) }
                        TextButton(enabled = ready, onClick = { nav.navigate("login") { launchSingleTop = true } }) { Text(t("login_button")) }
                    }
                }
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
    if (demo && notice != null) {
        AlertDialog(
            onDismissRequest = { DemoAccess.dismiss() },
            title = { Text(t("version_demo")) },
            text = { Text(t(notice!!)) },
            confirmButton = { TextButton(onClick = { DemoAccess.dismiss(); nav.navigate("registro") { launchSingleTop = true } }) { Text(t("demo_crear_cuenta")) } },
            dismissButton = { TextButton(onClick = { DemoAccess.dismiss() }) { Text(t("entendido")) } }
        )
    }
    LaunchedEffect(owner) { DemoAccess.dismiss() }
}

@Composable
fun DemoRestricted(nav: NavController, key: String = "demo_solo_registrados", content: @Composable () -> Unit) {
    if (!DemoAccess.isDemo()) { content(); return }
    val language by ConfigurationManager.idioma.collectAsState()
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(StringResourceManager.getString(key, language))
        Button(onClick = { nav.navigate("registro") { launchSingleTop = true } }) { Text(StringResourceManager.getString("demo_crear_cuenta", language)) }
        TextButton(onClick = { nav.popBackStack() }) { Text(StringResourceManager.getString("volver", language)) }
    }
}
