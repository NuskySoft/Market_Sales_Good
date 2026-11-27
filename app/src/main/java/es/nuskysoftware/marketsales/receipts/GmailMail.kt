package es.nuskysoftware.marketsales.receipts

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Gmail mediante Google Identity Services y Gmail API. La cuenta de correo no es la sesión
 * Firebase de Market Sales. Google administra los tokens; no se guarda ningún refresh token.
 */
object GmailMail {
    const val PROVIDER = "gmail_oauth"
    const val SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send"
    private const val EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"

    /** Fija siempre la cuenta elegida; no se usa implícitamente otra cuenta del dispositivo. */
    fun request(email:String):AuthorizationRequest = AuthorizationRequest.builder()
        .setAccount(Account(email,"com.google"))
        .setRequestedScopes(listOf(Scope(SEND_SCOPE),Scope(EMAIL_SCOPE),Scope("openid")))
        .build()

    /** Solicita el token a Google, sin interacción desde los trabajos de segundo plano. */
    private suspend fun token(context:Context,email:String):String {
        val result=try {
            withTimeoutOrNull(30_000L) { Identity.getAuthorizationClient(context).authorize(request(email)).await() }
                ?: throw MailFailure("PENDING","Google no respondió a tiempo.")
        } catch(e:CancellationException){throw e}
        catch(e:MailFailure){throw e}
        catch(e:ApiException){
            if(e.statusCode in listOf(CommonStatusCodes.NETWORK_ERROR,CommonStatusCodes.TIMEOUT,CommonStatusCodes.INTERNAL_ERROR))
                throw MailFailure("PENDING","Autorización Google temporalmente no disponible.")
            throw MailFailure("ERROR","Vuelve a conectar Gmail en Configuración de correo (Google "+e.statusCode+").")
        }
        if(result.hasResolution())throw MailFailure("ERROR","Google necesita que vuelvas a autorizar Gmail en Configuración de correo.")
        return accessToken(result)
    }

    /** Comprueba que se concedió el permiso de envío antes de utilizar cualquier token. */
    fun accessToken(result:AuthorizationResult):String {
        if(!result.grantedScopes.contains(SEND_SCOPE))throw MailFailure("ERROR","No se concedió a Market Sales el permiso de enviar correo.")
        return result.accessToken?.takeIf{it.isNotBlank()} ?: throw MailFailure("ERROR","Google no devolvió una autorización válida.")
    }

    private data class Reply(val status:Int,val body:JSONObject)

    /** HTTPS sin redirecciones ni registros de tokens, direcciones o contenido del mensaje. */
    private fun http(url:String,token:String,body:String?=null):Reply {
        val connection=URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects=false
            connection.connectTimeout=15_000;connection.readTimeout=30_000
            connection.setRequestProperty("Authorization","Bearer "+token)
            connection.setRequestProperty("Accept","application/json")
            if(body!=null){
                connection.requestMethod="POST"
                connection.doOutput=true
                connection.setRequestProperty("Content-Type","application/json; charset=UTF-8")
                val bytes=body.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use{it.write(bytes)}
            }
            val status=connection.responseCode
            val stream=if(status>=400)connection.errorStream else connection.inputStream
            val json=stream?.bufferedReader()?.use{it.readText()}.orEmpty()
            return Reply(status,runCatching{JSONObject(json)}.getOrDefault(JSONObject()))
        }finally{connection.disconnect()}
    }

    /** Verifica la identidad devuelta por Google; no requiere permiso de lectura del buzón. */
    private suspend fun identity(token:String):String=withContext(Dispatchers.IO){
        val reply=try{http("https://www.googleapis.com/oauth2/v3/userinfo",token)}
        catch(e:Exception){throw MailFailure("PENDING","No se pudo comprobar la cuenta de Google.")}
        if(reply.status==401)throw MailFailure("ERROR","La autorización de Google ha caducado.")
        if(reply.status==429 || reply.status>=500)throw MailFailure("PENDING","Google temporalmente no disponible.")
        val email=reply.body.optString("email")
        if(reply.status!=200 || !reply.body.optBoolean("email_verified") || !ReceiptMail.validAddress(email))
            throw MailFailure("ERROR","Google no confirmó una dirección de correo válida.")
        email
    }

    /** La configuración guarda exclusivamente el correo autorizado, no su token ni contraseña. */
    suspend fun configuration(result:AuthorizationResult,selected:String):MailConfig {
        val email=identity(accessToken(result))
        if(!email.equals(selected,true))throw MailFailure("ERROR","La cuenta autorizada no coincide con la seleccionada.")
        return MailConfig(PROVIDER,"",email,"",0,"",email,"")
    }

    /** El 401 es un rechazo inequívoco: se puede limpiar el token y repetir una sola vez. */
    private suspend fun clear(context:Context,token:String){
        try {
            withTimeoutOrNull(30_000L){Identity.getAuthorizationClient(context)
                .clearToken(ClearTokenRequest.builder().setToken(token).build()).await()}
        }catch(e:CancellationException){throw e}
        catch(_:Exception){throw MailFailure("ERROR","Vuelve a conectar Gmail para renovar la autorización.")}
    }

    /** Envío directo. Si una petición pudo ser aceptada, no se reenvía automáticamente. */
    suspend fun send(context:Context,config:MailConfig,recipient:String,file:File?,english:Boolean):MailConfig=withContext(Dispatchers.IO){
        val raw=try{GmailMessage.raw(config.address,recipient,file,english)}
        catch(e:Exception){throw MailFailure("ERROR","No se pudo preparar el correo o el PDF supera el tamaño admitido.")}
        val body=JSONObject().put("raw",raw).toString()
        repeat(2){attempt->
            val access=token(context,config.address)
            val actual=try{identity(access)}catch(e:MailFailure){
                if(attempt==0 && e.message=="La autorización de Google ha caducado."){
                    clear(context,access);return@repeat
                }
                throw e
            }
            if(!actual.equals(config.address,true))throw MailFailure("ERROR","La cuenta Google ya no coincide con el remitente autorizado.")
            val reply=try{http("https://gmail.googleapis.com/gmail/v1/users/me/messages/send",access,body)}
            catch(e:Exception){throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación del mensaje por Gmail.")}
            when {
                reply.status in 200..299 -> return@withContext config
                reply.status==401 && attempt==0 -> clear(context,access)
                reply.status==401 -> throw MailFailure("ERROR","Vuelve a conectar Gmail para renovar la autorización.")
                reply.status==429 -> throw MailFailure("PENDING","Gmail ha limitado temporalmente los envíos.")
                reply.status==403 -> {
                    val reason=reply.body.optJSONObject("error")?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason")
                    if(reason in setOf("rateLimitExceeded","userRateLimitExceeded","dailyLimitExceeded"))
                        throw MailFailure("PENDING","Gmail ha limitado temporalmente los envíos.")
                    throw MailFailure("ERROR","Gmail rechazó el permiso de envío o la API no está habilitada (HTTP 403).")
                }
                reply.status in 400..499 -> throw MailFailure("ERROR","Gmail rechazó el mensaje (HTTP "+reply.status+").")
                else -> throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación del mensaje por Gmail (HTTP "+reply.status+").")
            }
        }
        throw MailFailure("ERROR","Vuelve a autorizar Gmail.")
    }
}
