package es.nuskysoftware.marketsales.work

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.receipts.*
import es.nuskysoftware.marketsales.utils.AppModePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/** La sesión persistida y Firebase deben corresponder a la misma cuenta, también tras muerte del proceso. */
private suspend fun activeOwner(db:AppDatabase):String? {
    val uid=FirebaseAuth.getInstance().currentUser?.uid ?: return null
    if(AppModePolicy.isLocalOnly(uid) || db.configuracionDao().getUsuarioLogueado()!=uid)return null
    return uid
}

/** Generación PDF independiente de la red, de los correos y de proveedores de copia externa. */
class ReceiptPdfWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        lock.withLock {
            val db=AppDatabase.getDatabase(applicationContext)
            val owner=activeOwner(db) ?: return@withLock Result.success()
            val dao=db.receiptDocumentDao()
            // La instantánea es durable; si una generación falla, la recuperación periódica la reintentará.
            for(id in dao.documentIds(owner)){
                if(activeOwner(db)!=owner)break
                val local=ReceiptFiles.file(applicationContext,owner,id)
                if(local.isFile && local.length()>0)continue
                try{dao.find(owner,id)?.let{ReceiptFiles.render(applicationContext,it)}}
                catch(e:CancellationException){throw e}
                catch(_:Exception){/* La venta permanece guardada; la instantánea no se elimina. */}
            }
            runCatching{ReceiptWorker.requestCopy(applicationContext)}
            Result.success()
        }
    }
    companion object{private val lock=Mutex()}
}

/** Copias SAF separadas; un proveedor lento o revocado no bloquea la generación ni el envío. */
class ReceiptCopyWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        lock.withLock{
            val db=AppDatabase.getDatabase(applicationContext)
            val owner=activeOwner(db) ?: return@withLock Result.success()
            if(db.userDao().isPremium(owner)!=true)return@withLock Result.success()
            val settings=ReceiptSettings(applicationContext,owner)
            val destination=settings.folder ?: return@withLock Result.success()
            val dao=db.receiptDocumentDao()
            var retry=false
            for(id in dao.documentIds(owner)){
                if(activeOwner(db)!=owner || db.userDao().isPremium(owner)!=true || settings.folder!=destination)break
                val doc=dao.find(owner,id) ?: continue
                if(doc.copiedTo==destination)continue
                try{
                    val pdf=ReceiptFiles.render(applicationContext,doc)
                    ReceiptFiles.external(applicationContext,owner,id,Uri.parse(destination),pdf,false)
                    dao.copied(owner,id,destination)
                }catch(e:CancellationException){throw e}
                catch(_:Exception){retry=true}
            }
            if(retry)Result.retry() else Result.success()
        }
    }
    companion object{private val lock=Mutex()}
}

/** Correo directo: reclama las solicitudes por compare-and-set; nunca toca ventas ni stock. */
class ReceiptWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        lock.withLock{
            val db=AppDatabase.getDatabase(applicationContext)
            val owner=activeOwner(db) ?: return@withLock Result.success()
            val settings=ReceiptSettings(applicationContext,owner)
            val dao=db.receiptDocumentDao()
            val device=ReceiptSettings.deviceId(applicationContext)
            var retry=false
            // Un proceso muerto durante SMTP podría haber entregado el mensaje al servidor.
            dao.recoverInterrupted(owner,device)
            for(mail in dao.pending(owner,device)){
                if(activeOwner(db)!=owner)break
                if(db.recibosDao().obtenerReciboPorId(mail.receiptId,owner)==null){
                    dao.transition(owner,mail.id,"PENDING","CANCELLED","Recibo ya no disponible");continue
                }
                val config=settings.mail()
                if(config==null || !config.address.equals(mail.sender,true)){
                    dao.transition(owner,mail.id,"PENDING","ERROR","Configura la cuenta de correo original");continue
                }
                // Los envíos previamente autorizados continúan tras bajar a Free.
                val pdf=try{
                    val local=ReceiptFiles.file(applicationContext,owner,mail.receiptId)
                    if(local.isFile && local.length()>0)local else {
                        val doc=dao.find(owner,mail.receiptId) ?: error("PDF no disponible")
                        ReceiptFiles.render(applicationContext,doc)
                    }
                }catch(e:CancellationException){throw e}
                catch(_:Exception){retry=true;continue}
                if(dao.transition(owner,mail.id,"PENDING","SENDING","")!=1)continue
                try{
                    if(activeOwner(db)!=owner){dao.transition(owner,mail.id,"SENDING","PENDING","");break}
                    ReceiptMail.send(config,mail.recipient,pdf,settings,mail.receiptId.endsWith("-EN"))
                    dao.transition(owner,mail.id,"SENDING","SENT","Aceptado por el servidor")
                }catch(e:CancellationException){throw e}
                catch(e:MailFailure){
                    dao.transition(owner,mail.id,"SENDING",e.state,e.message.orEmpty())
                    if(e.state=="PENDING")retry=true
                }catch(_:Exception){dao.transition(owner,mail.id,"SENDING","UNCERTAIN","No se pudo confirmar el envío")}
            }
            if(retry)Result.retry() else Result.success()
        }
    }
    companion object{
        private val lock=Mutex()
        private fun network()=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        /** Recuperación periódica independiente para PDF, copias y correo; sin servicios de pago. */
        fun start(context:Context){
            val wm=WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork("receipt-pdf-periodic",ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(ReceiptPdfWorker::class.java,15,TimeUnit.MINUTES).build())
            wm.enqueueUniquePeriodicWork("receipt-copy-periodic",ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(ReceiptCopyWorker::class.java,15,TimeUnit.MINUTES).build())
            wm.enqueueUniquePeriodicWork("receipt-mail-periodic",ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(ReceiptWorker::class.java,15,TimeUnit.MINUTES).setConstraints(network()).build())
            request(context)
        }
        fun request(context:Context){
            WorkManager.getInstance(context).enqueueUniqueWork("receipt-pdf",ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequest.Builder(ReceiptPdfWorker::class.java).build())
            requestMail(context)
        }
        fun requestCopy(context:Context){
            WorkManager.getInstance(context).enqueueUniqueWork("receipt-copy",ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(ReceiptCopyWorker::class.java).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
        }
        fun requestMail(context:Context){
            WorkManager.getInstance(context).enqueueUniqueWork("receipt-mail",ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(ReceiptWorker::class.java).setConstraints(network())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
        }
    }
}
