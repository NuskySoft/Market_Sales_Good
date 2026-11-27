package es.nuskysoftware.marketsales.data.repository

import android.content.Context
import es.nuskysoftware.marketsales.data.local.database.AppDatabase
import es.nuskysoftware.marketsales.utils.AppModePolicy
import es.nuskysoftware.marketsales.utils.ConfigurationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

object AccountSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    @Synchronized
    fun start(context: Context) {
        if (job?.isActive == true) return
        val dao = AppDatabase.getDatabase(context.applicationContext).userDao()
        job = scope.launch {
            ConfigurationManager.usuarioLogueado.collectLatest { uid ->
                if (uid == null || AppModePolicy.isLocalOnly(uid)) return@collectLatest
                ConfigurationManager.markPremiumUnknown()
                dao.getUserByIdFlow(uid).collect { account ->
                    ConfigurationManager.applyAccountFromRoom(uid, account?.esPremium, account?.email, account?.displayName)
                }
            }
        }
    }
}
