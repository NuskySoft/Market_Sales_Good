// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/PantallaPerfil.kt
package es.nuskysoftware.marketsales.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import android.util.Log
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.repository.AuthState
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.FooterMarca
import es.nuskysoftware.marketsales.utils.StringResourceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // AuthViewModel para manejar actualizaciones
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context))
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Estados de ConfigurationManager
    val currentLanguage by ConfigurationManager.idioma.collectAsState()
    val usuarioEmail by ConfigurationManager.usuarioEmail.collectAsState()
    val displayName by ConfigurationManager.displayName.collectAsState()

    // Estados locales del formulario
    var nombre by remember { mutableStateOf(displayName ?: "") }
    var email by remember { mutableStateOf(usuarioEmail ?: "") }
    var passwordActual by remember { mutableStateOf("") }
    var passwordNueva by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }

    var isPasswordActualVisible by remember { mutableStateOf(false) }
    var isPasswordNuevaVisible by remember { mutableStateOf(false) }
    var isConfirmarPasswordVisible by remember { mutableStateOf(false) }

    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Estado de carga para botones individuales
    var isUpdatingInfo by remember { mutableStateOf(false) }
    var isUpdatingPassword by remember { mutableStateOf(false) }

    // Actualizar campos cuando cambie el usuario
    LaunchedEffect(currentUser, displayName, usuarioEmail) {
        nombre = displayName ?: ""
        email = usuarioEmail ?: ""
    }

    // ✅ NUEVO: Actualizar campos cuando se muestre el mensaje de éxito
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(300)
            val updatedDisplayName = ConfigurationManager.displayName.value
            val updatedEmail = ConfigurationManager.usuarioEmail.value

            Log.d("PantallaPerfil", "🔄 ConfigurationManager.displayName: $updatedDisplayName")
            Log.d("PantallaPerfil", "🔄 Campo nombre antes: $nombre")

            if (updatedDisplayName != null) {
                nombre = updatedDisplayName
            }
            if (updatedEmail != null) {
                email = updatedEmail
            }

            Log.d("PantallaPerfil", "🔄 Campo nombre después: $nombre")
        }
    }

    // Validaciones
    val isNombreValid = nombre.isNotBlank()
    val isEmailValid = email.contains("@") && email.contains(".")
    val isPasswordActualValid = passwordActual.isNotBlank()
    val isPasswordNuevaValid = passwordNueva.length >= 6
    val isConfirmarPasswordValid = passwordNueva == confirmarPassword
    val canUpdateBasicInfo = isNombreValid && isEmailValid
    val canUpdatePassword = isPasswordActualValid && isPasswordNuevaValid && isConfirmarPasswordValid

    // Manejar estados de AuthViewModel
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                showError = true
                showSuccess = false
                isUpdatingInfo = false
                isUpdatingPassword = false
            }
            is AuthState.Authenticated -> {
                isUpdatingInfo = false
                isUpdatingPassword = false
            }
            else -> { }
        }
    }

    LaunchedEffect(displayName) {
        Log.d("PantallaPerfil", "🔄 ConfigurationManager.displayName: $displayName")
        if (!displayName.isNullOrBlank()) {
            Log.d("PantallaPerfil", "🔄 Campo nombre antes: $nombre")
            nombre = displayName ?: ""  // ✅ Manejar el nullable
            Log.d("PantallaPerfil", "🔄 Campo nombre después: $nombre")
        }
    }
    LaunchedEffect(currentUser, displayName, usuarioEmail) {
        nombre = displayName ?: currentUser?.displayName ?: ""
        email = usuarioEmail ?: currentUser?.email ?: ""
        Log.d("PantallaPerfil", "🔄 Campos actualizados - nombre: $nombre, email: $email")
    }
    // Función para actualizar información básica
    fun updateBasicInfo() {
        scope.launch {
            isUpdatingInfo = true
            showError = false
            showSuccess = false

            try {
                val result = authViewModel.updateUserProfile(
                    displayName = nombre,
                    email = email
                )

                if (result) {
                    successMessage = StringResourceManager.getString("informacion_actualizada", currentLanguage)
                    showSuccess = true
                } else {
                    errorMessage = StringResourceManager.getString("error_actualizar_informacion", currentLanguage)
                    showError = true
                }
            } catch (e: Exception) {
                errorMessage = StringResourceManager.getString("error_generico", currentLanguage).replace("{0}", e.message ?: "")
                showError = true
            } finally {
                isUpdatingInfo = false
            }
        }
    }

    // Función para cambiar contraseña
    fun updatePassword() {
        scope.launch {
            isUpdatingPassword = true
            showError = false
            showSuccess = false

            try {
                val result = authViewModel.updatePassword(
                    currentPassword = passwordActual,
                    newPassword = passwordNueva
                )

                if (result) {
                    successMessage = StringResourceManager.getString("contrasena_actualizada", currentLanguage)
                    showSuccess = true
                    // Limpiar campos
                    passwordActual = ""
                    passwordNueva = ""
                    confirmarPassword = ""
                } else {
                    errorMessage = StringResourceManager.getString("error_cambiar_contrasena", currentLanguage)
                    showError = true
                }
            } catch (e: Exception) {
                errorMessage = StringResourceManager.getString("error_generico", currentLanguage).replace("{0}", e.message ?: "")
                showError = true
            } finally {
                isUpdatingPassword = false
            }
        }
    }

    // Auto-ocultar mensajes después de 5 segundos
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(5000)
            showSuccess = false
        }
    }

    LaunchedEffect(showError) {
        if (showError) {
            kotlinx.coroutines.delay(5000)
            showError = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringResourceManager.getString("perfil", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = StringResourceManager.getString("volver", currentLanguage)
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información de cuenta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = StringResourceManager.getString("informacion_cuenta", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = StringResourceManager.getString("modificar_informacion", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Mensajes de estado (arriba para mejor visibilidad)
                if (showError && errorMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showError = false }) {
                                Text("✕", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                if (showSuccess && successMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = successMessage,
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showSuccess = false }) {
                                Text("✕", color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }

                // Formulario de información básica
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = StringResourceManager.getString("informacion_personal", currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Nombre
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = {
                                nombre = it
                                showError = false
                                showSuccess = false
                            },
                            label = { Text(StringResourceManager.getString("nombre_completo", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = nombre.isNotEmpty() && !isNombreValid,
                            supportingText = {
                                if (nombre.isNotEmpty() && !isNombreValid) {
                                    Text(
                                        text = StringResourceManager.getString("nombre_vacio", currentLanguage),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isUpdatingInfo
                        )

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                showError = false
                                showSuccess = false
                            },
                            label = { Text(StringResourceManager.getString("correo_electronico", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            isError = email.isNotEmpty() && !isEmailValid,
                            supportingText = {
                                if (email.isNotEmpty() && !isEmailValid) {
                                    Text(
                                        text = StringResourceManager.getString("email_invalido", currentLanguage),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isUpdatingInfo
                        )

                        // Botón actualizar información básica
                        Button(
                            onClick = { updateBasicInfo() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canUpdateBasicInfo && !isUpdatingInfo && !isUpdatingPassword
                        ) {
                            if (isUpdatingInfo) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(StringResourceManager.getString("actualizando", currentLanguage))
                            } else {
                                Text(StringResourceManager.getString("actualizar_informacion", currentLanguage))
                            }
                        }
                    }
                }

                // Formulario de cambio de contraseña
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = StringResourceManager.getString("cambiar_contrasena_titulo", currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = StringResourceManager.getString("cambiar_contrasena_descripcion", currentLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        // Contraseña actual
                        OutlinedTextField(
                            value = passwordActual,
                            onValueChange = {
                                passwordActual = it
                                showError = false
                                showSuccess = false
                            },
                            label = { Text(StringResourceManager.getString("contrasena_actual", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordActualVisible = !isPasswordActualVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordActualVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordActualVisible)
                                            StringResourceManager.getString("ocultar_contrasena", currentLanguage)
                                        else
                                            StringResourceManager.getString("mostrar_contrasena", currentLanguage)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordActualVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = passwordActual.isNotEmpty() && !isPasswordActualValid,
                            supportingText = {
                                if (passwordActual.isNotEmpty() && !isPasswordActualValid) {
                                    Text(
                                        text = StringResourceManager.getString("contrasena_actual_requerida", currentLanguage),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isUpdatingPassword
                        )

                        // Nueva contraseña
                        OutlinedTextField(
                            value = passwordNueva,
                            onValueChange = {
                                passwordNueva = it
                                showError = false
                                showSuccess = false
                            },
                            label = { Text(StringResourceManager.getString("nueva_contrasena", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordNuevaVisible = !isPasswordNuevaVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordNuevaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordNuevaVisible)
                                            StringResourceManager.getString("ocultar_contrasena", currentLanguage)
                                        else
                                            StringResourceManager.getString("mostrar_contrasena", currentLanguage)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordNuevaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = passwordNueva.isNotEmpty() && !isPasswordNuevaValid,
                            supportingText = {
                                if (passwordNueva.isNotEmpty() && !isPasswordNuevaValid) {
                                    Text(
                                        text = StringResourceManager.getString("contrasena_minimo_caracteres", currentLanguage),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isUpdatingPassword
                        )

                        // Confirmar contraseña
                        OutlinedTextField(
                            value = confirmarPassword,
                            onValueChange = {
                                confirmarPassword = it
                                showError = false
                                showSuccess = false
                            },
                            label = { Text(StringResourceManager.getString("confirmar_nueva_contrasena", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isConfirmarPasswordVisible = !isConfirmarPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isConfirmarPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isConfirmarPasswordVisible)
                                            StringResourceManager.getString("ocultar_contrasena", currentLanguage)
                                        else
                                            StringResourceManager.getString("mostrar_contrasena", currentLanguage)
                                    )
                                }
                            },
                            visualTransformation = if (isConfirmarPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            isError = confirmarPassword.isNotEmpty() && !isConfirmarPasswordValid,
                            supportingText = {
                                if (confirmarPassword.isNotEmpty() && !isConfirmarPasswordValid) {
                                    Text(
                                        text = StringResourceManager.getString("contrasenas_no_coinciden", currentLanguage),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isUpdatingPassword
                        )

                        // Botón cambiar contraseña
                        Button(
                            onClick = { updatePassword() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canUpdatePassword && !isUpdatingPassword && !isUpdatingInfo
                        ) {
                            if (isUpdatingPassword) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(StringResourceManager.getString("cambiando", currentLanguage))
                            } else {
                                Text(StringResourceManager.getString("cambiar_contrasena_btn", currentLanguage))
                            }
                        }
                    }
                }
            }

            // Footer
            FooterMarca()
        }
    }
}
