// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaConfiguracion.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.repository.ConfiguracionRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FooterMarca
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    navController: NavController? = null
) {
    // Contexto, repo y scope para offline-first
    val context = LocalContext.current
    val repo = remember { ConfiguracionRepository(context) }
    val scope = rememberCoroutineScope()

    // Observar estados desde ConfigurationManager usando StateFlows
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val currentFont     by ConfigurationManager.fuente.collectAsState()
    val isDarkTheme     by ConfigurationManager.temaOscuro.collectAsState()
    val versionApp      by ConfigurationManager.versionApp.collectAsState()
    val usuarioEmail    by ConfigurationManager.usuarioEmail.collectAsState()

    // Calcular valores derivados
    val isPremium = versionApp == 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("configuracion", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información de cuenta
                item {
                    TarjetaInformacionCuenta(
                        email = usuarioEmail ?: "usuario@email.com",
                        esPremium = isPremium,
                        version = "V1.0",
                        language = currentLanguage
                    )
                }

                // Idioma (solo Premium) - Dropdown
                item {
                    var expandedIdioma by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPremium)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(if (isPremium) 4.dp else 2.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isPremium) {
                                        if (isPremium) expandedIdioma = !expandedIdioma
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_language),
                                    contentDescription = "Idioma",
                                    tint = if (isPremium)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = StringResourceManager.getString("idioma", currentLanguage),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPremium)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = if (currentLanguage == "es") "Español" else "English",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPremium)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    if (!isPremium) {
                                        Text(
                                            text = StringResourceManager.getString("funcion_premium", currentLanguage),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Red.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                if (isPremium) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (expandedIdioma) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                                        ),
                                        contentDescription = "Expandir",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (expandedIdioma && isPremium) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    listOf("es" to "Español", "en" to "English").forEach { (codigo, nombre) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    ConfigurationManager.setIdioma(codigo)
                                                    expandedIdioma = false
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = currentLanguage == codigo,
                                                onClick = {
                                                    ConfigurationManager.setIdioma(codigo)
                                                    expandedIdioma = false
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(nombre)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fuente (solo Premium) - Dropdown
                item {
                    var expandedFuente by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPremium)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(if (isPremium) 4.dp else 2.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isPremium) {
                                        if (isPremium) expandedFuente = !expandedFuente
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_font),
                                    contentDescription = "Fuente",
                                    tint = if (isPremium)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = StringResourceManager.getString("fuente", currentLanguage),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPremium)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = currentFont,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPremium)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    if (!isPremium) {
                                        Text(
                                            text = StringResourceManager.getString("funcion_premium", currentLanguage),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Red.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                if (isPremium) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (expandedFuente) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                                        ),
                                        contentDescription = "Expandir",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (expandedFuente && isPremium) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    listOf("Montserrat", "Poppins", "Roboto").forEach { fuente ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    ConfigurationManager.setFuente(fuente)
                                                    expandedFuente = false
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = currentFont == fuente,
                                                onClick = {
                                                    ConfigurationManager.setFuente(fuente)
                                                    expandedFuente = false
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(fuente)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tema Oscuro (solo Premium) - Switch
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPremium)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(if (isPremium) 4.dp else 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_palette),
                                contentDescription = "Tema",
                                tint = if (isPremium)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = StringResourceManager.getString("tema", currentLanguage),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isPremium)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = if (isDarkTheme) "Oscuro" else "Claro",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPremium)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { nuevoValor ->
                                    if (isPremium) {
                                        ConfigurationManager.setTemaOscuro(nuevoValor)
                                        scope.launch {
                                            repo.actualizarTema(nuevoValor)
                                        }
                                    }
                                },
                                enabled = isPremium
                            )
                        }
                    }
                }

                // Sección desarrollo
                item {
                    OpcionConfiguracion(
                        iconRes = R.drawable.ic_settings,
                        titulo = "Desarrollo",
                        subtitulo = if (isPremium) "🚀 Premium ACTIVO" else "✋ Modo FREE",
                        habilitado = true,
                        esPremium = false,
                        onClick = {
                            ConfigurationManager.setIsPremium(!isPremium)
                        },
                        language = currentLanguage
                    )
                }

                // Promoción Premium para usuarios FREE
                if (!isPremium) {
                    item {
                        TarjetaPromocionPremium(language = currentLanguage)
                    }
                }
            }

            // Botón Guardar
            Button(
                onClick = {
                    scope.launch {
                        repo.actualizarIdioma(currentLanguage)
                        repo.actualizarFuente(currentFont)
                        // temaOscuro ya se guardó al cambiar el Switch
                        usuarioEmail?.let { repo.actualizarUsuarioEmail(it) }
                        repo.sincronizar()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = StringResourceManager.getString("guardar", currentLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Footer
            FooterMarca()
        }
    }
}

@Composable
private fun TarjetaInformacionCuenta(
    email: String,
    esPremium: Boolean,
    version: String,
    language: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringResourceManager.getString("informacion_cuenta", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (esPremium) Color(0xFFFFD700) else Color(0xFF90EE90)
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = if (esPremium) "PREMIUM" else "FREE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "App $version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun OpcionConfiguracion(
    iconRes: Int,
    titulo: String,
    subtitulo: String?,
    habilitado: Boolean,
    esPremium: Boolean,
    onClick: () -> Unit,
    language: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado) { if (habilitado) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (habilitado)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(if (habilitado) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = titulo,
                tint = if (habilitado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (habilitado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                subtitulo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (habilitado) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                if (!habilitado && esPremium) {
                    Text(
                        text = StringResourceManager.getString("funcion_premium", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
            if (habilitado) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "Ir",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaPromocionPremium(language: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = "Premium",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = StringResourceManager.getString("promocion_premium", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = StringResourceManager.getString("desbloquea_funciones", language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

