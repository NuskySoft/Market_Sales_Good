package es.nuskysoftware.marketsales.ui.pantallas

// Responsabilidad: Pantalla de diagnóstico de datos, utilizada para inspeccionar el estado local y remoto durante el desarrollo.


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
// Pantalla de diagnóstico de datos, utilizada para inspeccionar el estado local y remoto durante el desarrollo.
fun PantallaDebugDatos(navController: NavController) {
    val currentLanguage = ConfigurationManager.idioma.collectAsState().value
    val scope = rememberCoroutineScope()
    var mensaje by remember { mutableStateOf<String?>(null) }

    fun cambiarActivo(activo: Boolean) {
        scope.launch {
            val userId = ConfigurationManager.getCurrentUserId() ?: return@launch
            val db = FirebaseFirestore.getInstance()
            val colecciones = listOf("mercadillos", "articulos", "categorias")
            try {
                for (col in colecciones) {
                    val campo = if (col == "categorias") "activa" else "activo"
                    val snap = db.collection(col)
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    for (doc in snap.documents) {
                        doc.reference.update(campo, activo).await()
                    }
                }
                mensaje = StringResourceManager.getString("datos_actualizados", currentLanguage)
            } catch (e: Exception) {
                mensaje = StringResourceManager.getString("error_detalle", currentLanguage).replace("{0}", e.message.orEmpty())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { cambiarActivo(true) }) { Text(StringResourceManager.getString("activar_datos", currentLanguage)) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { cambiarActivo(false) }) { Text(StringResourceManager.getString("desactivar_datos", currentLanguage)) }
        mensaje?.let { texto ->
            Spacer(Modifier.height(16.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) { Text(StringResourceManager.getString("volver", currentLanguage)) }
    }
}
