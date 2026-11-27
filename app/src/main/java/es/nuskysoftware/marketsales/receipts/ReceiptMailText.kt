package es.nuskysoftware.marketsales.receipts

/**
 * Textos comunes de todos los proveedores. Un correo sin PDF confirma la configuración
 * de la cuenta propia; nunca se presenta como un ticket o recibo de una venta ficticia.
 */
internal object ReceiptMailText {
    fun subject(hasPdf:Boolean,english:Boolean):String =
        if(hasPdf) { if(english) "Sales receipt" else "Recibo de venta" }
        else { if(english) "Market Sales - Email configured" else "Market Sales - Correo configurado" }

    fun body(hasPdf:Boolean,english:Boolean):String =
        if(hasPdf) { if(english) "Thank you for your purchase. Your receipt is attached." else "Gracias por su compra. Adjuntamos su recibo." }
        else { if(english) "Your email account is ready to send receipts with Market Sales. No sale has been recorded." else "Tu cuenta de correo está preparada para enviar recibos con Market Sales. No se ha registrado ninguna venta." }
}
