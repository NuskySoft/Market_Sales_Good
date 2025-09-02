package es.nuskysoftware.marketsales.ui.pantallas


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PantallaDebugDatos(navController: NavController) {
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
                mensaje = "Datos actualizados"
            } catch (e: Exception) {
                mensaje = "Error: ${e.message}"
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
        Button(onClick = { cambiarActivo(true) }) { Text("Activar datos") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { cambiarActivo(false) }) { Text("Desactivar datos") }
        mensaje?.let { texto ->
            Spacer(Modifier.height(16.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) { Text("Volver") }
    }
}
