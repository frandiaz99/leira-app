package com.fdm.plancito.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Grupos del usuario y el grupo activo, compartidos por todas las pantallas. */
class GroupStore(private val repo: Repository, private val prefs: Prefs, scope: CoroutineScope) {

    private val memberships = MutableStateFlow<List<Membership>?>(null)

    /** null = todavía no se cargaron. */
    val groups: StateFlow<List<Membership>?> = memberships.asStateFlow()

    val error = MutableStateFlow<String?>(null)

    /** El elegido; si ya no es miembro (o no eligió ninguno), el primero. */
    val active: StateFlow<Group?> = combine(memberships, prefs.activeGroupFlow) { list, id ->
        list?.firstOrNull { it.groupId == id }?.group ?: list?.firstOrNull()?.group
    }.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun refresh() {
        try {
            repo.ensureFreshSession()
            memberships.value = repo.myGroups()
            error.value = null
        } catch (e: Exception) {
            error.value = e.message ?: e.toString()
        }
    }

    fun select(groupId: String) = prefs.setActiveGroup(groupId)

    fun isOwner(groupId: String): Boolean = memberships.value?.any { it.groupId == groupId && it.role == "owner" } == true

    fun clear() {
        memberships.value = null
        prefs.setActiveGroup(null)
    }
}
