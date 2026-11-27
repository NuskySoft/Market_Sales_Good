package es.nuskysoftware.marketsales.work

import android.content.Context
import android.net.Uri
import androidx.work.*
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.receipts.*
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import es.nuskysoftware.marketsales.utils.AppModePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/** Generación y copia locales, y transporte de solicitudes autorizadas. Nunca escribe ventas ni stock. */
class ReceiptWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        lock.withLock {
            val database=AppDatabase.getDatabase(applicationContext)
            val owner=com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if(owner.isNullOrBlank() || AppModePolicy.isLocalOnly(owner)) return@withLock Result.success()
            // La sesión de Firebase y la cuenta persistida deben coincidir, también tras muerte del proceso.
            if(database.configuracionDao().getUsuarioLogueado()!=owner) return@withLock Result.success()
            suspend fun active() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid==owner && database.configuracionDao().getUsuarioLogueado()==owner
            val service=ReceiptService(applicationContext)
            val settings=service.settings(owner)
            val dao=database.receiptDocumentDao()
            var retry=false
            // Un proceso anterior pudo morir después de aceptar un correo; no se reenvía a ciegas.
            dao.recoverInterrupted(owner)
            for(id in dao.documentIds(owner)) {
                if(!active()) return@withLock Result.success()
                val doc=dao.find(owner,id) ?: continue
                try {
                    val pdf=ReceiptFiles.render(applicationContext,doc)
                    // Copias nuevas solo en Premium; los originales ya generados nunca se eliminan por descenso.
                    val destination=settings.folder
                    if(database.userDao().isPremium(owner)==true && destination!=null && doc.copiedTo!=destination) {
                        ReceiptFiles.external(applicationContext,owner,id,Uri.parse(destination),pdf,false)
                        dao.copied(owner,id,destination)
                    }
                } catch(e:CancellationException){throw e}
                catch(e:Exception){retry=true}
            }
            val pending=dao.pending(owner)
            if(pending.isNotEmpty() && !service.online()) return@withLock Result.retry()
            for(mail in pending) {
                if(!active()) return@withLock Result.success()
                if(database.recibosDao().obtenerReciboPorId(mail.receiptId,owner)==null) {
                    dao.transition(owner,mail.id,"PENDING","CANCELLED","Recibo ya no disponible");continue
                }
                val config=settings.mail()
                if(config==null || !config.address.equals(mail.sender,true)) {
                    dao.transition(owner,mail.id,"PENDING","ERROR","Configura la cuenta de correo original")
                    continue
                }
                // Los envíos ya autorizados continúan incluso si el usuario ha pasado a Free.
                val pdf=try {
                    val local=ReceiptFiles.file(applicationContext,owner,mail.receiptId)
                    if(local.isFile && local.length()>0) local else {
                        val doc=dao.find(owner,mail.receiptId) ?: error("PDF no disponible")
                        ReceiptFiles.render(applicationContext,doc)
                    }
                } catch(e:CancellationException){throw e}
                catch(e:Exception){retry=true;continue}
                if(dao.transition(owner,mail.id,"PENDING","SENDING","")!=1)continue
                try {
                    if(!active()) {
                        dao.transition(owner,mail.id,"SENDING","PENDING","");break
                    }
                    ReceiptMail.send(config,mail.recipient,pdf,settings,mail.receiptId.endsWith("-EN"))
                    dao.transition(owner,mail.id,"SENDING","SENT","Aceptado por el servidor")
                } catch(e:CancellationException){throw e}
                catch(e:MailFailure) {
                    dao.transition(owner,mail.id,"SENDING",e.state,e.message.orEmpty())
                    if(e.state=="PENDING")retry=true
                } catch(e:Exception) {
                    dao.transition(owner,mail.id,"SENDING","UNCERTAIN","No se pudo confirmar el envío")
                }
            }
            if(retry) Result.retry() else Result.success()
        }
    }
    companion object {
        private val lock=Mutex()
        /** Trabajo periódico de recuperación + petición inmediata tras venta o ajuste. */
        fun start(context:Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("receipt-local-periodic",
                ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequest.Builder(ReceiptWorker::class.java,15,TimeUnit.MINUTES).build())
            request(context)
        }
        fun request(context:Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("receipt-local",ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(ReceiptWorker::class.java)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
            // Una petición independiente condicionada por red despierta la cola al recuperar conexión.
            WorkManager.getInstance(context).enqueueUniqueWork("receipt-network",ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(ReceiptWorker::class.java)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
        }
    }
}
