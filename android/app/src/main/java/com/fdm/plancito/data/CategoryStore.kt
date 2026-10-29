package com.fdm.plancito.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Categorías del grupo activo (fijas + propias), compartidas por todas las pantallas. */
class CategoryStore(private val repo: Repository, groups: GroupStore, scope: CoroutineScope) {

    private val custom = MutableStateFlow<List<PlaceCategory>>(emptyList())
    private var groupId: String? = null

    /** Solo las propias del grupo activo. */
    val own: StateFlow<List<PlaceCategory>> = custom

    val all: StateFlow<List<PlaceCategory>> = custom.map { allCategories(it) }
        .stateIn(scope, SharingStarted.Eagerly, CATEGORIES)

    init {
        scope.launch {
            groups.active.collect { g ->
                groupId = g?.id
                custom.value = emptyList()
                refresh()
            }
        }
    }

    /** Si falla (sin internet, o falta la migración en Supabase) quedan las fijas. */
    suspend fun refresh() {
        val id = groupId ?: return
        runCatching { repo.categories(id) }.onSuccess { list -> custom.value = list.map { it.toPlaceCategory() } }
    }

    suspend fun create(label: String, iconName: String): PlaceCategory {
        val id = groupId ?: error("Elegí un grupo primero")
        val created = repo.createCategory(id, label.trim(), iconName).toPlaceCategory()
        custom.value = (custom.value + created).sortedBy { it.label.lowercase() }
        return created
    }

    /** Los lugares que la usaban pasan a "otro" (lo hace un trigger en la base). */
    suspend fun delete(category: PlaceCategory) {
        repo.deleteCategory(category.value)
        custom.value = custom.value - category
    }
}
