package es.nuskysoftware.marketsales.data.local.dao

import androidx.room.*
import es.nuskysoftware.marketsales.data.local.entity.ReceiptDocumentEntity
import es.nuskysoftware.marketsales.data.local.entity.ReceiptMailEntity
import kotlinx.coroutines.flow.Flow

/** Acceso a documentos y cola exclusivamente locales, siempre acotado al propietario. */
@Dao
interface ReceiptDocumentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: ReceiptDocumentEntity)
    @Query("SELECT * FROM receipt_documents WHERE owner=:owner AND receiptId=:id")
    suspend fun find(owner: String, id: String): ReceiptDocumentEntity?
    @Query("SELECT receiptId FROM receipt_documents WHERE owner=:owner")
    suspend fun documentIds(owner: String): List<String>
    @Query("SELECT receiptId FROM receipt_documents WHERE owner=:owner")
    fun ids(owner: String): Flow<List<String>>
    @Query("UPDATE receipt_documents SET copiedTo=:destination WHERE owner=:owner AND receiptId=:id")
    suspend fun copied(owner: String, id: String, destination: String)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun enqueue(mail: ReceiptMailEntity)
    @Query("SELECT * FROM receipt_mail WHERE owner=:owner ORDER BY created DESC")
    fun observeMail(owner: String): Flow<List<ReceiptMailEntity>>
    @Query("SELECT * FROM receipt_mail WHERE owner=:owner AND state='PENDING' ORDER BY created")
    suspend fun pending(owner: String): List<ReceiptMailEntity>
    // Compare-and-set: solo un trabajador puede reclamar o cancelar una solicitud.
    @Query("UPDATE receipt_mail SET state=:next, detail=:detail WHERE owner=:owner AND id=:id AND state=:expected")
    suspend fun transition(owner: String, id: String, expected: String, next: String, detail: String): Int
    // Un proceso muerto durante SMTP deja resultado incierto: nunca reenviar a ciegas.
    @Query("UPDATE receipt_mail SET state='UNCERTAIN', detail='Envío interrumpido' WHERE owner=:owner AND state='SENDING'")
    suspend fun recoverInterrupted(owner: String)
    @Query("DELETE FROM receipt_documents WHERE owner=:owner")
    suspend fun deleteDocuments(owner: String)
    @Query("DELETE FROM receipt_mail WHERE owner=:owner")
    suspend fun deleteMail(owner: String)
}
