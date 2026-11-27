package es.nuskysoftware.marketsales.ui.composables
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import es.nuskysoftware.marketsales.data.repository.*
import es.nuskysoftware.marketsales.utils.*
import es.nuskysoftware.marketsales.work.BusinessSyncScheduler
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecoveryDialogs() {
    val context = LocalContext.current
    val gate = remember { RecoveryGate(context) }
    val question by RecoveryGate.question.collectAsState()
    val uid by ConfigurationManager.usuarioLogueado.collectAsState()
    val lang by ConfigurationManager.idioma.collectAsState()
    var confirm by remember(question, uid) { mutableStateOf(false) }
    var premiumInfo by remember(question, uid) { mutableStateOf(false) }
    val q = question?.takeIf { it.uid == uid } ?: return
    fun tr(key: String) = StringResourceManager.getString(key, lang)
    if (premiumInfo) AlertDialog(onDismissRequest = { premiumInfo = false }, title = { Text(tr("go_premium")) },
        text = { Text(tr("premium_recovery_info")) },
        confirmButton = { TextButton(onClick = { premiumInfo = false; BusinessSyncScheduler.request(context) }) { Text(tr("aceptar")) } })
    else if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text(tr("permanent_delete_title"), color = MaterialTheme.colorScheme.error) }, text = { Text(tr("permanent_delete_warning")) },
        confirmButton = { TextButton(onClick = { gate.confirmReset(q.uid); confirm = false }) { Text(tr("permanent_delete_action"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("cancelar")) } })
    else AlertDialog(onDismissRequest = { gate.dismiss(q.uid) }, title = { Text(tr("recover_previous_data")) },
        text = { Text(tr(if (q.firstWrite) "recovery_last_chance" else "recovery_first_chance")) },
        confirmButton = { TextButton(onClick = { premiumInfo = true }) { Text(tr("go_premium")) } },
        dismissButton = { TextButton(onClick = { if (q.firstWrite) confirm = true else gate.dismiss(q.uid) }) { Text(tr("continue_free")) } })
}
