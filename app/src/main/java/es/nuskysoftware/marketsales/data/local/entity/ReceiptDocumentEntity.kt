package es.nuskysoftware.marketsales.data.local.entity

import androidx.room.Entity

/** Instantánea inmutable local, escrita junto con la venta Premium. No se sincroniza con Firebase. */
@Entity(tableName = "receipt_documents", primaryKeys = ["owner", "receiptId"])
data class ReceiptDocumentEntity(
    val owner: String, val receiptId: String, val snapshot: String,
    val copiedTo: String? = null
)

/** Solicitud de correo autorizada: conserva destinatario y estado incluso al cerrar la app. */
@Entity(tableName = "receipt_mail", primaryKeys = ["owner", "id"])
data class ReceiptMailEntity(
    val owner: String, val id: String, val receiptId: String, val recipient: String,
    val created: Long, val state: String = "PENDING", val detail: String = ""
)
