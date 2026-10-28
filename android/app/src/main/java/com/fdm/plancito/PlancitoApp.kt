package com.fdm.plancito

import android.app.Application
import com.fdm.plancito.data.GroupStore
import com.fdm.plancito.data.Prefs
import com.fdm.plancito.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.shareIn
import org.osmdroid.config.Configuration

class PlancitoApp : Application() {

    lateinit var repository: Repository
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var groups: GroupStore
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Cambios en la base (Realtime), compartidos por todas las pantallas. */
    val dbChanges: SharedFlow<Unit> by lazy {
        repository.changes()
            .catch { }
            .shareIn(appScope, SharingStarted.WhileSubscribed(5_000))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        repository = Repository()
        groups = GroupStore(repository, prefs, appScope)
        // Los servidores de tiles de OpenStreetMap piden identificar la app.
        Configuration.getInstance().userAgentValue = packageName
    }

    companion object {
        lateinit var instance: PlancitoApp
            private set
    }
}
