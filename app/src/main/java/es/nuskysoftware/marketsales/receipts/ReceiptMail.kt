package es.nuskysoftware.marketsales.receipts

import android.util.Base64
import es.nuskysoftware.marketsales.BuildConfig
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Properties
import javax.mail.*
import javax.mail.internet.*
import javax.activation.DataHandler
import javax.activation.FileDataSource

/** Resultado de transporte: solo se repite automáticamente un fallo inequívocamente anterior a la aceptación. */
class MailFailure(val state:String, message:String): Exception(message)

/** SMTP TLS y Microsoft Graph delegado, ambos directos desde el dispositivo. */
object ReceiptMail {
    fun validAddress(value:String):Boolean = runCatching {
        val a=InternetAddress(value,true); a.validate()
        a.address==value && value.contains("@") && !value.contains('\n') && !value.contains('\r')
    }.getOrDefault(false)

    fun send(config:MailConfig, recipient:String, file:File?, settings:ReceiptSettings, english:Boolean=false) {
        require(validAddress(recipient))
        if(config.provider=="outlook") {
            MicrosoftMail.send(config,recipient,file,settings,english); return
        }
        require(config.port in 1..65535 && config.host.isNotBlank() && config.tls in listOf("SSL","STARTTLS"))
        val props=Properties().apply {
            put("mail.smtp.auth","true"); put("mail.smtp.host",config.host); put("mail.smtp.port",config.port.toString())
            put("mail.smtp.ssl.checkserveridentity","true")
            put("mail.smtp.connectiontimeout","15000"); put("mail.smtp.timeout","30000"); put("mail.smtp.writetimeout","30000")
            if(config.tls=="SSL") put("mail.smtp.ssl.enable","true")
            else { put("mail.smtp.starttls.enable","true"); put("mail.smtp.starttls.required","true") }
        }
        val session=Session.getInstance(props)
        val message=MimeMessage(session).apply {
            setFrom(InternetAddress(config.address,config.name,"UTF-8"))
            setRecipient(Message.RecipientType.TO,InternetAddress(recipient,true))
            subject=if(file==null) "Market Sales - test" else if(english) "Sales receipt" else "Recibo de venta"
            val body=if(file==null) "Market Sales - OK" else if(english) "Thank you for your purchase." else "Gracias por su compra."
            if(file==null) setText(body,"UTF-8") else {
                val text=MimeBodyPart().apply { setText(body,"UTF-8") }
                val attachment=MimeBodyPart().apply { dataHandler=DataHandler(FileDataSource(file)); fileName="recibo.pdf" }
                setContent(MimeMultipart().apply { addBodyPart(text);addBodyPart(attachment) })
            }
            saveChanges()
        }
        val transport=session.getTransport("smtp")
        try {
            // Si no se estableció sesión SMTP, el mensaje aún no puede haber sido aceptado.
            try { transport.connect(config.host,config.port,config.username,config.secret) }
            catch(e:AuthenticationFailedException){throw MailFailure("ERROR","Autenticación rechazada")}
            catch(e:MessagingException){
                if(generateSequence<Throwable>(e){it.cause}.any { it is javax.net.ssl.SSLException })
                    throw MailFailure("ERROR","TLS no válido")
                throw MailFailure("PENDING","Conexión SMTP no disponible")
            }
            // Tras iniciar DATA, un corte de red puede ocurrir después de aceptar el correo.
            try { transport.sendMessage(message,message.allRecipients) }
            catch(e:SendFailedException){
                if(e.validSentAddresses?.isNotEmpty()==true) return
                if(e.invalidAddresses?.isNotEmpty()==true) throw MailFailure("ERROR","Destinatario rechazado")
                throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación SMTP")
            }
            catch(e:MessagingException){throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación SMTP")}
        } finally { runCatching { transport.close() } }
    }
}

/** Cliente público Microsoft: autorización en navegador, sin contraseña ni secreto de aplicación. */
object MicrosoftMail {
    private const val BASE="https://login.microsoftonline.com/common/oauth2/v2.0/"
    private const val SCOPES="https://graph.microsoft.com/Mail.Send https://graph.microsoft.com/User.Read offline_access"
    val available get()=BuildConfig.MS_MAIL_CLIENT_ID.isNotBlank()
    data class DeviceCode(val code:String,val device:String,val url:String,val seconds:Int,val interval:Int)
    data class Reply(val status:Int,val body:JSONObject)

    private fun request(url:String,method:String,body:String?=null,token:String?=null,json:Boolean=false):Reply {
        val connection=URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects=false
            connection.requestMethod=method; connection.connectTimeout=15000;connection.readTimeout=30000
            token?.let { connection.setRequestProperty("Authorization","Bearer "+it) }
            if(body!=null) {
                connection.doOutput=true
                connection.setRequestProperty("Content-Type",if(json) "application/json" else "application/x-www-form-urlencoded")
                connection.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
            }
            val status=connection.responseCode
            val text=(if(status>=400) connection.errorStream else connection.inputStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
            return Reply(status,runCatching{JSONObject(text)}.getOrDefault(JSONObject()))
        } finally { connection.disconnect() }
    }
    private fun form(vararg fields:Pair<String,String>)=fields.joinToString("&"){URLEncoder.encode(it.first,"UTF-8")+"="+URLEncoder.encode(it.second,"UTF-8")}
    fun begin():DeviceCode {
        check(available){"Outlook pendiente de registrar en Microsoft"}
        val r=request(BASE+"devicecode","POST",form("client_id" to BuildConfig.MS_MAIL_CLIENT_ID,"scope" to SCOPES))
        check(r.status==200){"No se pudo iniciar autorización"}
        return DeviceCode(r.body.getString("user_code"),r.body.getString("device_code"),r.body.getString("verification_uri"),r.body.getInt("expires_in"),r.body.optInt("interval",5))
    }
    /** Espera únicamente durante la sesión explícita de autorización, respetando el intervalo del proveedor. */
    suspend fun authorize(code:DeviceCode):MailConfig {
        val end=System.currentTimeMillis()+code.seconds*1000L;var interval=code.interval.coerceAtLeast(5)
        while(System.currentTimeMillis()<end) {
            delay(interval*1000L)
            val r=request(BASE+"token","POST",form("client_id" to BuildConfig.MS_MAIL_CLIENT_ID,"grant_type" to "urn:ietf:params:oauth:grant-type:device_code","device_code" to code.device))
            if(r.status==200) {
                val me=request("https://graph.microsoft.com/v1.0/me","GET",token=r.body.getString("access_token"))
                check(me.status==200)
                val address=me.body.optString("mail").ifBlank { me.body.getString("userPrincipalName") }
                check(ReceiptMail.validAddress(address))
                return MailConfig("outlook",me.body.optString("displayName"),address,"",0,"",address,r.body.getString("refresh_token"))
            }
            when(r.body.optString("error")){"authorization_pending"->Unit;"slow_down"->interval+=5;else->error("Autorización no completada")}
        }
        error("Autorización caducada")
    }
    fun send(config:MailConfig,recipient:String,file:File?,settings:ReceiptSettings,english:Boolean) {
        val r=try { request(BASE+"token","POST",form("client_id" to BuildConfig.MS_MAIL_CLIENT_ID,"grant_type" to "refresh_token","refresh_token" to config.secret,"scope" to SCOPES)) }
        catch(e:Exception){throw MailFailure("PENDING","Conexión Microsoft no disponible")}
        if(r.status==429 || r.status>=500) throw MailFailure("PENDING","Microsoft temporalmente no disponible")
        if(r.status!=200) throw MailFailure("ERROR","Vuelve a autorizar Outlook")
        val updated=r.body.optString("refresh_token")
        if(updated.isNotBlank() && updated!=config.secret) settings.save(config.copy(secret=updated))
        val token=r.body.getString("access_token")
        val message=JSONObject().put("subject",if(file==null) "Market Sales - test" else if(english) "Sales receipt" else "Recibo de venta")
            .put("body",JSONObject().put("contentType","Text").put("content",if(english) "Thank you for your purchase." else "Gracias por su compra."))
            .put("toRecipients",JSONArray().put(JSONObject().put("emailAddress",JSONObject().put("address",recipient))))
        if(file!=null) {
            if(file.length()>2_500_000L) throw MailFailure("ERROR","PDF demasiado grande para el envío directo de Outlook")
            message.put("attachments",JSONArray().put(JSONObject().put("@odata.type","#microsoft.graph.fileAttachment")
                .put("name","recibo.pdf").put("contentType","application/pdf").put("contentBytes",Base64.encodeToString(file.readBytes(),Base64.NO_WRAP))))
        }
        val result=try{request("https://graph.microsoft.com/v1.0/me/sendMail","POST",JSONObject().put("message",message).toString(),token,true)}
        catch(e:Exception){throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación por Microsoft")}
        when(result.status){
            202->return
            429->throw MailFailure("PENDING","Límite temporal de Microsoft")
            400,401,403,413->throw MailFailure("ERROR","Microsoft rechazó la solicitud")
            else->throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación por Microsoft")
        }
    }
}
