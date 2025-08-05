package es.nuskysoftware.marketsales.ui.pantallas

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.repository.AuthState
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModel
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModelFactory
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager
import es.nuskysoftware.marketsales.utils.DefaultBootstrapper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLogin(
    onNavigateToMain: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // ✅ Usa applicationContext para evitar fugas
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context.applicationContext)
    )

    val authState by authViewModel.authState.collectAsState()

    // Para forzar recomposición al cambiar idioma
    val currentLanguage by ConfigurationManager.idioma.collectAsState()

    // ===== Bootstrap usuario_default si Room está vacío =====
    LaunchedEffect(Unit) {
        DefaultBootstrapper.runIfNeeded(context.applicationContext)
    }

    // ===== Mostrar aviso claro al cerrar sesión =====
    var prevAuthState by remember { mutableStateOf<AuthState?>(null) }
    LaunchedEffect(authState) {
        if (prevAuthState is AuthState.Authenticated && authState is AuthState.Unauthenticated) {
            Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_LONG).show()
        }
        prevAuthState = authState
    }

    // Estados locales
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) } // true = Login, false = Registro
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Validaciones
    val isEmailValid = remember(email) { email.contains("@") && email.contains(".") }
    val isPasswordValid = remember(password) { password.length >= 6 }
    val isFormValid = isEmailValid && isPasswordValid

    // 🚀 GOOGLE AUTH - usa tu helper existente
    val googleSignInClient = remember {
        try {
            es.nuskysoftware.marketsales.utils.GoogleAuthHelper.getGoogleSignInClient(context)
        } catch (e: Exception) {
            null // Si falla, Google Auth no disponible
        }
    }

    // Launcher para Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    authViewModel.signInWithGoogle(idToken)
                } else {
                    errorMessage = "Error obteniendo token de Google"
                    showError = true
                }
            } catch (e: ApiException) {
                errorMessage = "Error en Google Sign-In: ${e.message}"
                showError = true
            }
        }
    }

    // Navegar tras autenticación
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> onNavigateToMain()
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                showError = true
            }
            else -> { /* no-op */ }
        }
    }

    fun handleAuthAction() {
        if (!isFormValid) return
        showError = false
        if (isLoginMode) authViewModel.loginWithEmail(email.trim(), password)
        else authViewModel.registerWithEmail(email.trim(), password)
    }

    fun handleGoogleSignIn() {
        if (googleSignInClient != null) {
            showError = false
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } else {
            errorMessage = "Google Auth no disponible - Revisa configuración"
            showError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(50.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_store),
                        contentDescription = StringResourceManager.getString("app_name", currentLanguage),
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            // Título App
            Text(
                text = StringResourceManager.getString("app_name", currentLanguage),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Subtítulo Login/Registro
            Text(
                text = if (isLoginMode)
                    StringResourceManager.getString("login_subtitle", currentLanguage)
                else
                    StringResourceManager.getString("register_subtitle", currentLanguage),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Formulario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            showError = false
                        },
                        label = { Text(StringResourceManager.getString("email", currentLanguage)) },
                        placeholder = { Text(StringResourceManager.getString("email_placeholder", currentLanguage)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        isError = email.isNotEmpty() && !isEmailValid,
                        supportingText = {
                            if (email.isNotEmpty() && !isEmailValid) {
                                Text(
                                    text = StringResourceManager.getString("email_invalid", currentLanguage),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showError = false
                        },
                        label = { Text(StringResourceManager.getString("password", currentLanguage)) },
                        placeholder = { Text(StringResourceManager.getString("password_placeholder", currentLanguage)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible)
                                        "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (isFormValid) handleAuthAction()
                        }),
                        isError = password.isNotEmpty() && !isPasswordValid,
                        supportingText = {
                            if (password.isNotEmpty() && !isPasswordValid) {
                                Text(
                                    text = StringResourceManager.getString("password_invalid", currentLanguage),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Error genérico
                    if (showError && errorMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Botón principal
            Button(
                onClick = { handleAuthAction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid && authState !is AuthState.Loading,
                shape = RoundedCornerShape(28.dp)
            ) {
                if (authState is AuthState.Loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            StringResourceManager.getString("loading", currentLanguage),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = if (isLoginMode)
                            StringResourceManager.getString("login_button", currentLanguage)
                        else
                            StringResourceManager.getString("register_button", currentLanguage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Divisor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    StringResourceManager.getString("or", currentLanguage),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // 🚀 Google Sign-In (tu estilo)
            if (googleSignInClient != null) {
                OutlinedButton(
                    onClick = { handleGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = authState !is AuthState.Loading,
                    shape = RoundedCornerShape(28.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_store),
                                contentDescription = "Google",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isLoginMode)
                                    StringResourceManager.getString("google_signin", currentLanguage)
                                else
                                    StringResourceManager.getString("google_register", currentLanguage),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Toggle Login/Registro
            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    showError = false
                    errorMessage = ""
                }
            ) {
                Text(
                    text = if (isLoginMode)
                        StringResourceManager.getString("go_to_register", currentLanguage)
                    else
                        StringResourceManager.getString("go_to_login", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Beneficios del registro (solo en modo registro)
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = StringResourceManager.getString("register_benefits_title", currentLanguage),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("benefit_1", "benefit_2", "benefit_3", "benefit_4").forEach { benefit ->
                            Text(
                                text = StringResourceManager.getString(benefit, currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

