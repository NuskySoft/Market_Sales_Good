package es.nuskysoftware.marketsales.utils
import android.content.Context
import es.nuskysoftware.marketsales.data.repository.SyncEngine
object FullBootstrapper {
    suspend fun runIfNeeded(context: Context, rawUserId: String?): Boolean {
        if (!DemoAccess.canSync(rawUserId) || !ConfigurationManager.getIsPremium()) return false
        SyncEngine(context).sync(rawUserId ?: return false)
        return true
    }
}
