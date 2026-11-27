package es.nuskysoftware.marketsales.receipts

import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

/** Serializa RFC 5322/MIME y base64url para Gmail API, sin depender de JavaMail ni de un servidor SMTP. */
internal object GmailMessage {
    private const val CRLF="\r\n"
    private const val MAX_PDF=10_000_000L
    private fun encoded(bytes:ByteArray)=Base64.getMimeEncoder(76,CRLF.toByteArray(Charsets.US_ASCII)).encodeToString(bytes)

    /** Las direcciones ya se validan en ReceiptMail; se bloquean igualmente saltos de línea en cabeceras. */
    fun raw(sender:String,recipient:String,file:File?,english:Boolean):String {
        require(listOf(sender,recipient).all{it.isNotBlank() && it.all{c->c.code in 33..126} && '@' in it})
        if(file!=null)require(file.isFile && file.length() in 1..MAX_PDF)
        val subject=if(file==null)"Market Sales - test" else if(english)"Sales receipt" else "Recibo de venta"
        val body=if(file==null)"Market Sales - OK" else if(english)"Thank you for your purchase." else "Gracias por su compra."
        val boundary="marketsales-"+UUID.randomUUID()
        val message=buildString{
            fun line(value:String=""){append(value);append(CRLF)}
            line("From: "+sender);line("To: "+recipient);line("Subject: "+subject)
            line("Date: "+ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
            line("Message-ID: <"+UUID.randomUUID()+"@marketsales.local>")
            line("MIME-Version: 1.0")
            if(file!=null){line("Content-Type: multipart/mixed; boundary=\""+boundary+"\"");line();line("--"+boundary)}
            line("Content-Type: text/plain; charset=UTF-8")
            line("Content-Transfer-Encoding: base64");line();line(encoded(body.toByteArray(Charsets.UTF_8)))
            if(file!=null){
                line("--"+boundary)
                line("Content-Type: application/pdf; name=\"recibo.pdf\"")
                line("Content-Disposition: attachment; filename=\"recibo.pdf\"")
                line("Content-Transfer-Encoding: base64");line();line(encoded(file.readBytes()))
                line("--"+boundary+"--")
            }
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(message.toByteArray(Charsets.UTF_8))
    }
}
