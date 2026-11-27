package es.nuskysoftware.marketsales.receipts

import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Diagnóstico independiente de JavaMail para SMTP con TLS implícito.
 * Solo se ejecuta tras una prueba de configuración fallida, nunca durante el envío
 * automático. No autentica, no envía comandos SMTP ni transmite credenciales.
 */
internal object SmtpConnectionProbe {
    fun check(host:String,port:Int):String {
        var phase="TCP"
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host,port),5000)
                socket.soTimeout=5000
                phase="TLS"
                val factory=SSLSocketFactory.getDefault() as SSLSocketFactory
                // Conservar la confianza del sistema Android y verificar el nombre del servidor.
                (factory.createSocket(socket,host,port,true) as SSLSocket).use { tls ->
                    tls.soTimeout=5000
                    tls.sslParameters=tls.sslParameters.apply { endpointIdentificationAlgorithm="HTTPS" }
                    tls.startHandshake()
                    phase="Saludo SMTP"
                    // Leer únicamente los tres dígitos del saludo: no conservar datos del servidor.
                    val input=tls.inputStream
                    val code=CharArray(3)
                    for(i in code.indices){
                        val value=input.read()
                        if(value !in 48..57)return "TCP y TLS correctos; saludo SMTP no válido."
                        code[i]=value.toChar()
                    }
                    val status=String(code)
                    if(status=="220")"TCP y TLS correctos en Android; servidor SMTP responde 220."
                    else "TCP y TLS correctos en Android; saludo SMTP $status."
                }
            }
        } catch(e:Exception) {
            // Solo el nombre de la clase: no mostrar mensajes crudos, IP, usuario o contraseña.
            "Prueba Android: fallo en $phase ("+e.javaClass.simpleName+")."
        }
    }
}
