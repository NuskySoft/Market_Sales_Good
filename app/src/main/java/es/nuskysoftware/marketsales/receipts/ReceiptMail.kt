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
import javax.activation.MailcapCommandMap
import javax.mail.util.ByteArrayDataSource
import com.sun.mail.smtp.SMTPTransport
import com.sun.mail.smtp.SMTPAddressFailedException
import com.sun.mail.smtp.SMTPSendFailedException
import java.io.OutputStream
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import javax.activation.UnsupportedDataTypeException

/** Resultado de transporte: solo se repite automáticamente un fallo inequívocamente anterior a la aceptación. */
class MailFailure(val state:String, message:String): Exception(message)

/** Selecciona Gmail API, SMTP TLS o Microsoft Graph, directamente desde el dispositivo. */
object ReceiptMail {
    fun validAddress(value:String):Boolean = runCatching {
        val a=InternetAddress(value,true); a.validate()
        a.address==value && value.contains("@") && !value.contains('\n') && !value.contains('\r')
    }.getOrDefault(false)

    /** Recorre también nextException de JavaMail: el error de red suele estar anidado ahí. */
    private fun causes(error:Throwable):List<Throwable> {
        val result=mutableListOf<Throwable>()
        val pending=java.util.ArrayDeque<Throwable>()
        pending.add(error)
        while(pending.isNotEmpty() && result.size<20) {
            val current=pending.removeFirst()
            if(result.any{it===current})continue
            result.add(current)
            current.cause?.let{pending.add(it)}
            (current as? MessagingException)?.nextException?.let{pending.add(it)}
        }
        return result
    }

    /** Diagnóstico seguro: solo etiquetas y códigos numéricos SMTP, nunca respuestas crudas ni secretos. */
    private fun connectFailure(error:Exception,config:MailConfig,code:Int):MailFailure {
        val chain=causes(error)
        val suffix=if(code in 400..599) " (SMTP $code)" else ""
        return when {
            chain.any{it is AuthenticationFailedException} || code==535 || code==534 ->
                MailFailure("ERROR",if(config.provider=="gmail") "Gmail rechazó la autenticación. Comprueba la contraseña de aplicación y la cuenta." + suffix else "El servidor rechazó el usuario o la contraseña." + suffix)
            chain.any{it is SSLException || it is java.security.cert.CertificateException} ->
                MailFailure("ERROR","No se pudo verificar TLS. Comprueba el servidor, certificado, puerto y cifrado.")
            chain.any{it is UnknownHostException} -> MailFailure("PENDING","No se pudo resolver el nombre del servidor SMTP. Comprueba el servidor y DNS.")
            chain.any{it is SocketTimeoutException} -> MailFailure("PENDING","Tiempo de conexión SMTP agotado. Comprueba puerto, cifrado y acceso a Internet.")
            chain.any{it is ConnectException} -> MailFailure("PENDING","Conexión SMTP rechazada. Comprueba el puerto y si la red permite SMTP.")
            code in 500..599 -> MailFailure("ERROR","El servidor rechazó la conexión SMTP." + suffix)
            else -> MailFailure("PENDING","No se pudo establecer la conexión SMTP." + suffix)
        }
    }

    /** Registrar los manejadores MIME de forma explícita para no depender de META-INF en el APK. */
    private fun mimeHandlers() = MailcapCommandMap().apply {
        addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
        addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true")
    }

    suspend fun send(config:MailConfig, recipient:String, file:File?, settings:ReceiptSettings, english:Boolean=false, persistRefresh:Boolean=true):MailConfig {
        require(validAddress(recipient) && validAddress(config.address))
        // El correo de confirmación sin PDF solo se puede dirigir a la propia cuenta.
        require(file!=null || recipient.equals(config.address,true))
        require(config.name.none { it=='\r' || it=='\n' })
        if(config.provider==GmailMail.PROVIDER) return GmailMail.send(settings,config,recipient,file,english,persistRefresh)
        if(config.provider=="gmail")throw MailFailure("ERROR","Conecta Gmail con Google en Configuración de correo. La contraseña de aplicación ya no se utiliza.")
        if(config.provider=="outlook") return MicrosoftMail.send(config,recipient,file,settings,english,persistRefresh)
        if(config.port !in 1..65535 || config.host.isBlank() || config.tls !in listOf("SSL","STARTTLS"))
            throw MailFailure("ERROR","Configuración SMTP incompleta.")
        if((config.port==465 && config.tls!="SSL") || (config.port==587 && config.tls!="STARTTLS"))
            throw MailFailure("ERROR","El puerto 465 requiere SSL/TLS y el 587 utiliza STARTTLS.")
        // Las corrutinas de Android pueden ejecutar JavaMail con un cargador de clases ajeno a la app.
        // Se limita el cambio al hilo de esta operación y se restaura siempre.
        val thread=Thread.currentThread()
        val previousLoader=thread.contextClassLoader
        try {
            thread.contextClassLoader=ReceiptMail::class.java.classLoader
            val props=Properties().apply {
                put("mail.smtp.auth","true"); put("mail.smtp.host",config.host); put("mail.smtp.port",config.port.toString())
                put("mail.smtp.ssl.checkserveridentity","true")
                put("mail.smtp.connectiontimeout","15000"); put("mail.smtp.timeout","30000"); put("mail.smtp.writetimeout","30000")
                // Nunca registrar diálogo SMTP: podría contener datos privados de autenticación.
                put("mail.debug","false")
                if(config.tls=="SSL") put("mail.smtp.ssl.enable","true")
                else { put("mail.smtp.starttls.enable","true"); put("mail.smtp.starttls.required","true") }
            }
            val session=Session.getInstance(props)
            val message=try {
                val handlers=mimeHandlers()
                // ByteArrayDataSource evita depender de un manejador de objetos String para texto plano.
                val body=ReceiptMailText.body(file!=null,english)
                fun textHandler()=DataHandler(ByteArrayDataSource(body.toByteArray(Charsets.UTF_8),"text/plain; charset=UTF-8")).apply { setCommandMap(handlers) }
                MimeMessage(session).apply {
                    setFrom(InternetAddress(config.address,config.name,"UTF-8"))
                    setRecipient(Message.RecipientType.TO,InternetAddress(recipient,true))
                    subject=ReceiptMailText.subject(file!=null,english)
                    if(file==null) dataHandler=textHandler() else {
                        val text=MimeBodyPart().apply { dataHandler=textHandler() }
                        val attachment=MimeBodyPart().apply {
                            dataHandler=DataHandler(object:FileDataSource(file){override fun getContentType()="application/pdf"})
                            fileName="recibo.pdf"
                        }
                        setContent(MimeMultipart().apply { addBodyPart(text);addBodyPart(attachment) })
                        dataHandler.setCommandMap(handlers)
                    }
                    saveChanges()
                    // Verificar la serialización antes de conectar: un fallo MIME nunca se confunde con un envío incierto.
                    writeTo(object:OutputStream(){override fun write(b:Int)=Unit;override fun write(b:ByteArray,off:Int,len:Int)=Unit})
                }
            } catch(e:Exception) {
                val mime=causes(e).any{it is UnsupportedDataTypeException || it is ClassNotFoundException}
                throw MailFailure("ERROR",if(mime) "Error interno al preparar el formato MIME del correo." else "Error interno al preparar el mensaje de correo.")
            }
            // Instanciar el proveedor SMTP conocido evita depender del descubrimiento de proveedores del APK.
            val transport=SMTPTransport(session,URLName("smtp",config.host,config.port,null,null,null))
            try {
                try { transport.connect(config.host,config.port,config.username,config.secret) }
                catch(e:Exception){
                    val code=transport.lastReturnCode
                    val failure=connectFailure(e,config,code)
                    // La clasificación genérica ocultaba la excepción original. Mostrar solo tipos y
                    // código SMTP, nunca mensajes crudos que pudieran incluir información de cuenta.
                    if(file==null){
                        val types=causes(e).map{it.javaClass.simpleName}.filter{it.isNotBlank()}.distinct().take(6).joinToString(" > ")
                        val diagnostic=if(config.tls=="SSL" && failure.state!="ERROR")
                            SmtpConnectionProbe.check(config.host,config.port) else ""
                        val details=listOf(failure.message.orEmpty(),
                            "Diagnóstico JavaMail: $types; SMTP $code.",diagnostic)
                            .filter{it.isNotBlank()}.joinToString("\n")
                        throw MailFailure(failure.state,details)
                    }
                    throw failure
                }
                try { transport.sendMessage(message,message.allRecipients) }
                catch(e:SendFailedException){
                    if(e.validSentAddresses?.isNotEmpty()==true) return config
                    val failures=causes(e)
                    val rc=failures.filterIsInstance<SMTPAddressFailedException>().firstOrNull()?.returnCode
                        ?: failures.filterIsInstance<SMTPSendFailedException>().firstOrNull()?.returnCode ?: transport.lastReturnCode
                    if(e.invalidAddresses?.isNotEmpty()==true) throw MailFailure("ERROR","Destinatario rechazado" + if(rc in 400..599) " (SMTP $rc)" else "")
                    // Un corte después de DATA puede haber entregado el mensaje: no reintentar automáticamente.
                    throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación SMTP" + if(rc in 400..599) " (SMTP $rc)" else "")
                }
                catch(e:MessagingException){throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación SMTP")}
            } finally { runCatching { transport.close() } }
            return config
        } finally { thread.contextClassLoader=previousLoader }
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
        return DeviceCode(r.body.getString("user_code"),r.body.getString("device_code"),"https://microsoft.com/devicelogin",r.body.getInt("expires_in"),r.body.optInt("interval",5))
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
    fun send(config:MailConfig,recipient:String,file:File?,settings:ReceiptSettings,english:Boolean,persistRefresh:Boolean):MailConfig {
        val r=try { request(BASE+"token","POST",form("client_id" to BuildConfig.MS_MAIL_CLIENT_ID,"grant_type" to "refresh_token","refresh_token" to config.secret,"scope" to SCOPES)) }
        catch(e:Exception){throw MailFailure("PENDING","Conexión Microsoft no disponible")}
        if(r.status==429 || r.status>=500) throw MailFailure("PENDING","Microsoft temporalmente no disponible")
        if(r.status!=200) throw MailFailure("ERROR","Vuelve a autorizar Outlook")
        val updated=r.body.optString("refresh_token")
        val refreshed=if(updated.isNotBlank()) config.copy(secret=updated) else config
        if(persistRefresh && refreshed!=config && settings.mail()==config) settings.save(refreshed)
        val token=r.body.getString("access_token")
        val message=JSONObject().put("subject",ReceiptMailText.subject(file!=null,english))
            .put("body",JSONObject().put("contentType","Text").put("content",ReceiptMailText.body(file!=null,english)))
            .put("toRecipients",JSONArray().put(JSONObject().put("emailAddress",JSONObject().put("address",recipient))))
        if(file!=null) {
            if(file.length()>2_500_000L) throw MailFailure("ERROR","PDF demasiado grande para el envío directo de Outlook")
            message.put("attachments",JSONArray().put(JSONObject().put("@odata.type","#microsoft.graph.fileAttachment")
                .put("name","recibo.pdf").put("contentType","application/pdf").put("contentBytes",Base64.encodeToString(file.readBytes(),Base64.NO_WRAP))))
        }
        val result=try{request("https://graph.microsoft.com/v1.0/me/sendMail","POST",JSONObject().put("message",message).toString(),token,true)}
        catch(e:Exception){throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación por Microsoft")}
        when(result.status){
            202->return refreshed
            429->throw MailFailure("PENDING","Límite temporal de Microsoft")
            400,401,403,413->throw MailFailure("ERROR","Microsoft rechazó la solicitud")
            else->throw MailFailure("UNCERTAIN","No se pudo confirmar la aceptación por Microsoft")
        }
    }
}
