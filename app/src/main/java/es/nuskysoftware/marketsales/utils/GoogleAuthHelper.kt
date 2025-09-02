// app/src/main/java/es/nuskysoftware/marketsales/utils/GoogleAuthHelper.kt
package es.nuskysoftware.marketsales.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import es.nuskysoftware.marketsales.R

/**
 * Helper class para manejar Google Sign-In de forma centralizada
 * V9 - Google Auth implementation (usa default_web_client_id)
 */
object GoogleAuthHelper {

    /**
     * Obtiene el cliente de Google Sign-In configurado
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        // IMPORTANTE: usar el ID web inyectado por google-services por buildType/flavor
        val defaultWebClientId = context.getString(R.string.default_web_client_id)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(defaultWebClientId) // <- clave: default_web_client_id
            .requestEmail()
            .requestProfile()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Verifica si hay un usuario ya logueado con Google
     */
    fun getLastSignedInAccount(context: Context) =
        GoogleSignIn.getLastSignedInAccount(context)

    /**
     * Cierra sesión de Google
     */
    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        getGoogleSignInClient(context).signOut().addOnCompleteListener { onComplete() }
    }

    /**
     * Revoca el acceso de Google
     */
    fun revokeAccess(context: Context, onComplete: () -> Unit = {}) {
        getGoogleSignInClient(context).revokeAccess().addOnCompleteListener { onComplete() }
    }
}


//package es.nuskysoftware.marketsales.utils
//
//import android.content.Context
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInClient
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import es.nuskysoftware.marketsales.R
//
///**
// * Helper class para manejar Google Sign-In de forma centralizada
// * V8 - Google Auth implementation
// */
//object  GoogleAuthHelper {
//
//    /**
//     * Obtiene el cliente de Google Sign-In configurado
//     */
//    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
//        val webClientId = context.getString(R.string.web_client_id)
//
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(webClientId)
//            .requestEmail()
//            .requestProfile()
//            .build()
//
//        return GoogleSignIn.getClient(context, gso)
//    }
//
//    /**
//     * Verifica si hay un usuario ya logueado con Google
//     */
//    fun getLastSignedInAccount(context: Context) = GoogleSignIn.getLastSignedInAccount(context)
//
//    /**
//     * Cierra sesión de Google
//     */
//    fun signOut(context: Context, onComplete: () -> Unit = {}) {
//        getGoogleSignInClient(context).signOut().addOnCompleteListener {
//            onComplete()
//        }
//    }
//
//    /**
//     * Revoca el acceso de Google
//     */
//    fun revokeAccess(context: Context, onComplete: () -> Unit = {}) {
//        getGoogleSignInClient(context).revokeAccess().addOnCompleteListener {
//            onComplete()
//        }
//    }
//}