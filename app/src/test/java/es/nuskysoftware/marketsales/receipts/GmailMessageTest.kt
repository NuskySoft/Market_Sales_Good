package es.nuskysoftware.marketsales.receipts

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

/** Pruebas JVM del formato de la API Gmail; no necesitan autorización ni conexión real. */
class GmailMessageTest {
    private fun decode(raw:String)=String(Base64.getUrlDecoder().decode(raw),Charsets.UTF_8)
    @Test fun testMailWithoutAttachment(){
        val raw=GmailMessage.raw("shop@example.com","shop@example.com",null,false)
        assertTrue(raw.matches(Regex("[A-Za-z0-9_-]+")))
        val mime=decode(raw)
        assertTrue(mime.contains("Subject: Market Sales - test\r\n"))
        assertTrue(mime.contains("Content-Type: text/plain; charset=UTF-8"))
        assertFalse(mime.contains("multipart/mixed"))
        assertTrue(mime.contains(Base64.getEncoder().encodeToString("Market Sales - OK".toByteArray())))
    }
    @Test fun receiptWithPdf(){
        val pdf=File.createTempFile("receipt",".pdf")
        try{
            val bytes="%PDF-1.4\n%%EOF".toByteArray()
            pdf.writeBytes(bytes)
            val mime=decode(GmailMessage.raw("shop@example.com","client@example.com",pdf,false))
            assertTrue(mime.contains("To: client@example.com\r\n"))
            assertTrue(mime.contains("Subject: Recibo de venta\r\n"))
            assertTrue(mime.contains("Content-Type: multipart/mixed; boundary="))
            assertTrue(mime.contains("filename=\"recibo.pdf\""))
            assertTrue(mime.contains(Base64.getEncoder().encodeToString(bytes)))
        }finally{pdf.delete()}
    }
    @Test fun rejectHeaderInjection(){
        assertTrue(runCatching{GmailMessage.raw("shop@example.com\r\nBcc: other@example.com","client@example.com",null,false)}.isFailure)
    }
}
