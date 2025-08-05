package es.nuskysoftware.marketsales.ui.pantallas

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import es.nuskysoftware.marketsales.R
import es.nuskysoftware.marketsales.data.repository.AuthState
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit
) {
    val TAG = "LoginScreen"

    // Estado de auth del repo
    val authState by viewModel.authState.collectAsState()

    // Campos email/password simples (mantengo por si usas login por email)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // --------- Google Sign-In setup ---------
    val context = LocalContext.current
    // usa el client id que genera Firebase (strings.xml)
    val serverClientId = stringResource(id = R.string.default_web_client_id)

    val gso = remember(serverClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId) // <- necesario para Firebase Auth
            .requestEmail()
            .build()
    }
    val googleClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (!idToken.isNullOrEmpty()) {
                viewModel.signInWithGoogle(idToken)
            } else {
                Log.e(TAG, "Google ID Token nulo/vacío")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In falló: code=${e.statusCode}", e)
        }
    }
    // ----------------------------------------

    // Navega cuando quede autenticado
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoggedIn()
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(24.dp))

            // --- Email/Password (opcional) ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.loginWithEmail(email.trim(), password) },
                    modifier = Modifier.weight(1f),
                    enabled = authState !is AuthState.Loading
                ) {
                    Text("Iniciar sesión")
                }
                OutlinedButton(
                    onClick = { viewModel.registerWithEmail(email.trim(), password) },
                    modifier = Modifier.weight(1f),
                    enabled = authState !is AuthState.Loading
                ) {
                    Text("Registrarse")
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Separador ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text("  o  ")
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // --- Botón Google ---
            Button(
                onClick = {
                    // opcional: cerrar sesión previa en el cliente para forzar selector
                    googleClient.signOut()
                    googleLauncher.launch(googleClient.signInIntent)
                },
                enabled = authState !is AuthState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar con Google")
            }

            Spacer(Modifier.height(24.dp))

            // --- Estado / errores ---
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.Error -> {
                    val msg = (authState as AuthState.Error).message
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> Unit
            }
        }
    }
}
