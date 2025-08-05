// PantallaConfiguracion.kt V10 - SIMPLIFICADO PARA SISTEMA MONOUSUARIO
package es.nuskysoftware.marketsales.ui.pantallas

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import es.nuskysoftware.marketsales.data.repository.AuthRepository
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FooterMarca
import es.nuskysoftware.marketsales.utils.StringResourceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

//    // Estados V10 simplificados
//    val currentLanguage by ConfigurationManager.idioma.collectAsState()
//    val currentFont by ConfigurationManager.fuente.collectAsState()
//    val isDarkTheme by ConfigurationManager.temaOscuro.collectAsState()
//    val currentMoneda by ConfigurationManager.moneda.collectAsState()
//    val esPremium by ConfigurationManager.esPremium.collectAsState()
//    val usuarioEmail by ConfigurationManager.usuarioEmail.collectAsState()
//    val isAuthenticated by ConfigurationManager.isAuthenticated.collectAsState()

    // Estados V10 simplificados
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val currentFont by ConfigurationManager.fuente.collectAsState()
    val isDarkTheme by ConfigurationManager.temaOscuro.collectAsState()
    val currentMoneda by ConfigurationManager.moneda.collectAsState()
    val esPremium by ConfigurationManager.esPremium.collectAsState()
    val usuarioEmail by ConfigurationManager.usuarioEmail.collectAsState()
    val isAuthenticated by ConfigurationManager.isAuthenticated.collectAsState()

    // Permisos V10 simplificados
    //val canChangeAdvanced = ConfigurationManager.canChangeConfiguration()
    val canChangeAdvanced = esPremium // En lugar de ConfigurationManager.canChangeConfiguration()

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
                            imageVector = Icons.Default.ArrowBack,
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
                // Información de cuenta V10
                item {
                    TarjetaInformacionCuenta(
                        email = usuarioEmail ?: if (isAuthenticated) "usuario@email.com" else "Usuario Invitado",
                        esPremium = esPremium,
                        isAuthenticated = isAuthenticated,
                        version = "V10.0",
                        language = currentLanguage
                    )
                }

                // Idioma (solo Premium)
                item {
                    OpcionConfiguracionV10(
                        iconRes = R.drawable.ic_language,
                        titulo = StringResourceManager.getString("idioma", currentLanguage),
                        valorActual = if (currentLanguage == "es") "Español" else "English",
                        opciones = listOf("es" to "Español", "en" to "English"),
                        habilitado = canChangeAdvanced,
                        onSeleccionar = { codigo ->
                            scope.launch {
                                authRepo.updateConfiguration(idioma = codigo)

                            }
                        }
                    )
                }

                // Fuente (solo Premium)
                item {
                    OpcionConfiguracionV10(
                        iconRes = R.drawable.ic_font,
                        titulo = StringResourceManager.getString("fuente", currentLanguage),
                        valorActual = currentFont,
                        opciones = listOf("Montserrat" to "Montserrat", "Poppins" to "Poppins", "Roboto" to "Roboto"),
                        habilitado = canChangeAdvanced,
                        onSeleccionar = { fuente ->
                            scope.launch {
                                authRepo.updateConfiguration(fuente = fuente)
                            }
                        }
                    )
                }

                // Tema Oscuro (todos pueden cambiar)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp)
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = StringResourceManager.getString("tema", currentLanguage),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isDarkTheme) "Oscuro" else "Claro",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { nuevoValor ->
                                    scope.launch {
                                        authRepo.updateConfiguration(temaOscuro = nuevoValor)
                                    }
                                }
                            )
                        }
                    }
                }

                // Moneda (solo Premium)
                item {
                    val monedas = listOf(
                        "€ Euro" to "€ Euro",
                        "$ Dólar" to "$ Dólar",
                        "£ Libra" to "£ Libra",
                        "$ Peso Argentino" to "$ Peso Argentino",
                        "$ Peso Mexicano" to "$ Peso Mexicano",
                        "$ Peso Colombiano" to "$ Peso Colombiano",
                        "S/ Sol Peruano" to "S/ Sol Peruano",
                        "$ Peso Chileno" to "$ Peso Chileno",
                        "Bs Bolívar" to "Bs Bolívar",
                        "$ Real Brasileño" to "$ Real Brasileño"
                    )

                    OpcionConfiguracionV10(
                        iconRes = R.drawable.ic_money,
                        titulo = "Moneda",
                        valorActual = currentMoneda,
                        opciones = monedas,
                        habilitado = canChangeAdvanced,
                        onSeleccionar = { moneda ->
                            scope.launch {
                                authRepo.updateConfiguration(moneda = moneda)
                            }
                        }
                    )
                }

                // Sección desarrollo/testing
//                item {
//                    Card(
//                        modifier = Modifier.fillMaxWidth().clickable {
//                            scope.launch {
//                                authRepo.updateUserPremium(!esPremium)
//                            }
//                        },
//                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Icon(
//                                painter = painterResource(id = R.drawable.ic_settings),
//                                contentDescription = "Desarrollo",
//                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
//                                modifier = Modifier.size(24.dp)
//                            )
//                            Spacer(modifier = Modifier.width(16.dp))
//                            Column(modifier = Modifier.weight(1f)) {
//                                Text(
//                                    text = "Desarrollo",
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    fontWeight = FontWeight.Medium,
//                                    color = MaterialTheme.colorScheme.onSecondaryContainer
//                                )
//                                Text(
//                                    text = if (esPremium) "🚀 Premium ACTIVO" else "✋ Modo FREE",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
//                                )
//                            }
//                        }
//                    }
//                }

                // Reemplazar la sección de desarrollo/testing en PantallaConfiguracion.kt:

// Sección desarrollo/testing - ✅ TOGGLE PREMIUM ARREGLADO
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            // ✅ CAMBIO: Usar función simplificada para desarrollo
                            ConfigurationManager.togglePremiumForDevelopment()
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (esPremium) {
                                Color(0xFFFFD700).copy(alpha = 0.3f) // Dorado para Premium
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer // Normal para Free
                            }
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (esPremium) 8.dp else 4.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (esPremium) R.drawable.ic_info else R.drawable.ic_settings
                                ),
                                contentDescription = "Desarrollo",
                                tint = if (esPremium) {
                                    Color(0xFFFFD700) // Dorado para Premium
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔧 Toggle Premium (Desarrollo)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (esPremium) {
                                        Color(0xFFFFD700)
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                                Text(
                                    text = if (esPremium) "🚀 PREMIUM ACTIVO - Tap para FREE" else "✋ MODO FREE - Tap para PREMIUM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (esPremium) {
                                        Color(0xFFFFD700).copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    }
                                )
                                if (esPremium) {
                                    Text(
                                        text = "• Idiomas adicionales\n• Fuentes personalizadas\n• Monedas múltiples",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = Color(0xFFFFD700).copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Indicador visual del estado
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esPremium) Color(0xFFFFD700) else Color.Gray
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
                    }
                }

                // Promoción Premium para usuarios FREE
                if (!esPremium) {
                    item {
                        TarjetaPromocionPremium(language = currentLanguage)
                    }
                }
            }

            // Footer
            FooterMarca()
        }
    }
    // 🔍 LOG TEMPORAL para debugging
    LaunchedEffect(esPremium, canChangeAdvanced) {
        Log.d("PantallaConfiguracion", "🔍 Estado actual:")
        Log.d("PantallaConfiguracion", "   - esPremium: $esPremium")
        Log.d("PantallaConfiguracion", "   - canChangeAdvanced: $canChangeAdvanced")
        Log.d("PantallaConfiguracion", "   - isAuthenticated: $isAuthenticated")
    }
}

@Composable
private fun OpcionConfiguracionV10(
    iconRes: Int,
    titulo: String,
    valorActual: String,
    opciones: List<Pair<String, String>>,
    habilitado: Boolean,
    onSeleccionar: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (habilitado)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(if (habilitado) 4.dp else 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = habilitado) {
                        if (habilitado) expanded = !expanded
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = titulo,
                    tint = if (habilitado)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (habilitado)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = valorActual,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (habilitado)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    if (!habilitado) {
                        Text(
                            text = "Solo Premium",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
                if (habilitado) {
                    Icon(
                        painter = painterResource(
                            id = if (expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                        ),
                        contentDescription = "Expandir",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (expanded && habilitado) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    opciones.forEach { (codigo, nombre) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSeleccionar(codigo)
                                    expanded = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = valorActual == codigo,
                                onClick = {
                                    onSeleccionar(codigo)
                                    expanded = false
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

@Composable
private fun TarjetaInformacionCuenta(
    email: String,
    esPremium: Boolean,
    isAuthenticated: Boolean,
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
                        containerColor = when {
                            !isAuthenticated -> Color(0xFFCCCCCC)
                            esPremium -> Color(0xFFFFD700)
                            else -> Color(0xFF90EE90)
                        }
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = when {
                            !isAuthenticated -> "INVITADO"
                            esPremium -> "PREMIUM"
                            else -> "FREE"
                        },
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
