// app/src/main/java/es/nuskysoftware/marketsales/ui/pantallas/LoginScreen.kt
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
import es.nuskysoftware.marketsales.pingFirestoreProd
import es.nuskysoftware.marketsales.ui.viewmodel.AuthViewModel
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.StringResourceManager

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit
) {
    val TAG = "LoginScreen"

    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val serverClientId = stringResource(id = R.string.default_web_client_id)

    val gso = remember(serverClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
    }
    val googleClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

//    val googleLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
//        try {
//            val account = task.getResult(ApiException::class.java)
//            val idToken = account.idToken
//            if (!idToken.isNullOrEmpty()) {
//                viewModel.signInWithGoogle(idToken)
//            } else {
//                Log.e(TAG, "Google ID Token nulo/vacío")
//            }
//        } catch (e: ApiException) {
//            Log.e(TAG, "Google Sign-In falló: code=${e.statusCode}", e)
//        }
//    }
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d(TAG, "GSI OK. email=${account.email}, idTokenNull=${idToken.isNullOrEmpty()}, serverClientId=$serverClientId")

            if (!idToken.isNullOrEmpty()) {
                viewModel.signInWithGoogle(idToken)
            } else {
                Log.e(TAG, "Google ID Token es null/vacío → suele ser CLIENT_ID/SHA-1 incorrectos (código 10).")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In falló: status=${e.statusCode}", e)
        }
    }


    // Navega cuando quede autenticado
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated){
            pingFirestoreProd()
            onLoggedIn()
        }
    }

    val currentLanguage by ConfigurationManager.idioma.collectAsState()

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

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = {
                    Text(
                        StringResourceManager.getString("email", currentLanguage)
                            .ifBlank { "Email" }
                    )
                },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        StringResourceManager.getString("contrasena", currentLanguage)
                            .ifBlank { "Contraseña" }
                    )
                },
                singleLine = true,
                enabled = !isLoading,
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
                    enabled = !isLoading
                ) {
                    Text(
                        StringResourceManager.getString("iniciar_sesion", currentLanguage)
                            .ifBlank { "Iniciar sesión" }
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.registerWithEmail(email.trim(), password) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(
                        StringResourceManager.getString("registrarse", currentLanguage)
                            .ifBlank { "Registrarse" }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Separador
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    " ${StringResourceManager.getString("o", currentLanguage).ifBlank { "o" }} "
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // Google
            Button(
                onClick = {
                    googleClient.signOut()
                    googleLauncher.launch(googleClient.signInIntent)
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    StringResourceManager.getString("continuar_google", currentLanguage)
                        .ifBlank { "Continuar con Google" }
                )
            }

            Spacer(Modifier.height(24.dp))

            when (authState) {
                is AuthState.Loading -> CircularProgressIndicator()
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
