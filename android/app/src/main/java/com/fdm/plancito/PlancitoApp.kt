package com.fdm.plancito

import android.app.Application
import com.fdm.plancito.data.CategoryStore
import com.fdm.plancito.data.GroupStore
import com.fdm.plancito.data.Prefs
import com.fdm.plancito.data.Repository
import com.fdm.plancito.data.SharedPlace
import com.fdm.plancito.notify.Notifier
import com.fdm.plancito.notify.Push
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.shareIn

/** [groupId]: el grupo del lugar, para pasar a ese grupo antes de abrirlo. */
data class OpenPlace(val placeId: String, val groupId: String?)

class PlancitoApp : Application() {

    lateinit var repository: Repository
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var groups: GroupStore
        private set
    lateinit var categories: CategoryStore
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Algo compartido desde otra app que todavía no se cargó como lugar. */
    val pendingShare = MutableStateFlow<SharedPlace?>(null)

    /** Lugar a abrir al tocar una notificación. */
    val pendingOpen = MutableStateFlow<OpenPlace?>(null)

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
        categories = CategoryStore(repository, groups, appScope)
        Notifier.createChannels(this)
        Push.init(this)
    }

    companion object {
        lateinit var instance: PlancitoApp
            private set
    }
}
