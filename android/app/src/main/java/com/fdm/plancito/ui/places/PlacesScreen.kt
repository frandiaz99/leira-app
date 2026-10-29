package com.fdm.plancito.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.data.Dates
import com.fdm.plancito.data.Place
import com.fdm.plancito.data.PlaceOrder
import com.fdm.plancito.data.ordersFor
import com.fdm.plancito.data.priceLabel
import com.fdm.plancito.data.sortPlaces
import com.fdm.plancito.notify.Reminders
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.EmptyState
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.GroupAvatar
import com.fdm.plancito.ui.common.icon
import com.fdm.plancito.ui.common.rememberCategories
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.Pill
import com.fdm.plancito.ui.common.Segment
import com.fdm.plancito.ui.common.SegmentedControl
import com.fdm.plancito.ui.common.StarRating
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Lugares del grupo activo (lo comparten Lugares y Mapa). */
class PlacesViewModel : BaseViewModel() {
    /** null = cargando el grupo por primera vez. */
    val places = MutableStateFlow<List<Place>?>(null)
    val covers = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        viewModelScope.launch {
            groups.active.map { it?.id }.distinctUntilChanged().collect { id ->
                places.value = null
                if (id != null) refresh()
            }
        }
        observeChanges()
    }

    override suspend fun load() {
        val groupId = groups.active.value?.id ?: return
        val list = repo.places(groupId)
        // Si mientras tanto se cambió de grupo, esta respuesta ya no sirve.
        if (groups.active.value?.id != groupId) return
        places.value = list
        val app = PlancitoApp.instance
        Reminders.sync(app, list, app.prefs.reminders)
        covers.value = repo.signedUrls(list.mapNotNull { it.cover?.storagePath })
    }
}

@Composable
fun PlacesScreen(onOpen: (String) -> Unit, onAdd: () -> Unit, onManageGroups: () -> Unit) {
    val vm: PlacesViewModel = viewModel()
    val places by vm.places.collectAsState()
    val covers by vm.covers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var visitedTab by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var order by rememberSaveable { mutableStateOf(PlaceOrder.Planned) }
    var sorting by remember { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf<Place?>(null) }

    val all = places.orEmpty()
    val usedCategories = rememberCategories().filter { c -> all.any { it.category == c.value } }
    val allTags = all.flatMap { it.tags }.distinct().sorted()
    val q = search.trim().lowercase()
    val filtered = sortPlaces(
        all.filter { p ->
            p.visited == visitedTab &&
                (category == null || p.category == category) &&
                (tag == null || tag in p.tags) &&
                (q.isEmpty() || "${p.name} ${p.address.orEmpty()} ${p.description.orEmpty()}".lowercase().contains(q))
        },
        visitedTab,
        order.takeIf { it in ordersFor(visitedTab) } ?: PlaceOrder.Planned,
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                GroupSwitcher(Modifier.weight(1f), onManageGroups)
                // "¿A dónde vamos?": uno al azar entre los pendientes que se ven (respeta los filtros).
                if (!visitedTab && filtered.size > 1) {
                    IconButton(onClick = { suggestion = filtered.random() }) {
                        Icon(Icons.Outlined.Casino, "¿A dónde vamos? Elegir uno al azar")
                    }
                }
                Box {
                    IconButton(onClick = { sorting = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Ordenar") }
                    DropdownMenu(expanded = sorting, onDismissRequest = { sorting = false }) {
                        ordersFor(visitedTab).forEach { o ->
                            DropdownMenuItem(
                                text = { Text(if (o == PlaceOrder.Planned && visitedTab) "Última visita" else o.label) },
                                trailingIcon = { if (o == order) Icon(Icons.Filled.Check, null) },
                                onClick = { order = o; sorting = false },
                            )
                        }
                    }
                }
                if (all.size > 3) {
                    IconButton(onClick = { searching = !searching; if (!searching) search = "" }) {
                        Icon(Icons.Filled.Search, "Buscar")
                    }
                }
            }
            LoadingBar(loading && places == null)

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SegmentedControl(
                            segments = listOf(
                                Segment(false, "Pendientes (${all.count { !it.visited }})"),
                                Segment(true, "Visitados (${all.count { it.visited }})"),
                            ),
                            selected = visitedTab,
                            onSelect = { visitedTab = it },
                        )
                        if (searching) {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                placeholder = { Text("Buscar…") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (usedCategories.size > 1 || allTags.isNotEmpty()) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (usedCategories.size > 1) usedCategories.forEach { c ->
                                    FilterChip(
                                        selected = category == c.value,
                                        onClick = { category = if (category == c.value) null else c.value },
                                        label = { Text(c.label) },
                                        leadingIcon = { Icon(c.icon, null, modifier = Modifier.size(18.dp)) },
                                    )
                                }
                                allTags.forEach { t ->
                                    FilterChip(selected = tag == t, onClick = { tag = if (tag == t) null else t }, label = { Text("#$t") })
                                }
                            }
                        }
                        ErrorText(error)
                    }
                }
                if (places != null && filtered.isEmpty()) {
                    item { Empty(visitedTab, hasAny = all.isNotEmpty(), filtering = category != null || tag != null || q.isNotEmpty(), onAdd) }
                }
                items(filtered, key = { it.id }) { p -> PlaceCard(p, p.cover?.let { covers[it.storagePath] }, onClick = { onOpen(p.id) }) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Agregar") },
            containerColor = Plancito.colors.primary,
            contentColor = Plancito.colors.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    suggestion?.let { p ->
        AlertDialog(
            onDismissRequest = { suggestion = null },
            icon = { Icon(Icons.Outlined.Casino, null) },
            title = { Text("¿Y si van a ${p.name}?") },
            text = {
                val cat = categoryInfo(p.category, rememberCategories())
                Text(
                    listOfNotNull(cat.label, priceLabel(p.price), p.address?.substringBefore(','))
                        .joinToString(" · "),
                )
            },
            confirmButton = { TextButton(onClick = { suggestion = null; onOpen(p.id) }) { Text("Ver") } },
            dismissButton = {
                TextButton(onClick = { suggestion = (filtered - p).randomOrNull() ?: p }) { Text("Otro") }
            },
        )
    }
}

/** Avatar + "Con Sofi" + flecha: cambia el grupo activo. */
@Composable
fun GroupSwitcher(modifier: Modifier = Modifier, onManageGroups: () -> Unit) {
    val store = PlancitoApp.instance.groups
    val memberships by store.groups.collectAsState()
    val active by store.active.collectAsState()
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            active?.let { GroupAvatar(it.id, it.name) }
            Text(
                active?.name ?: "Plancito",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.KeyboardArrowDown, "Cambiar de grupo")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            memberships.orEmpty().forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.group.name) },
                    leadingIcon = { GroupAvatar(m.group.id, m.group.name, size = 28.dp) },
                    trailingIcon = { if (m.groupId == active?.id) Icon(Icons.Filled.Check, null) },
                    onClick = { store.select(m.groupId); open = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Administrar grupos") }, onClick = { open = false; onManageGroups() })
        }
    }
}

@Composable
private fun PlaceCard(place: Place, coverUrl: String?, onClick: () -> Unit) {
    val t = Plancito.colors
    val cat = categoryInfo(place.category, rememberCategories())
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(t.surface).clickable(onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(t.primaryTint), contentAlignment = Alignment.Center) {
            if (coverUrl != null) {
                AsyncImage(coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp))
            } else {
                Icon(cat.icon, null, tint = t.primaryText, modifier = Modifier.size(30.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(cat.label, priceLabel(place.price), place.address?.substringBefore(',')).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = t.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val avg = place.averageRating
            when {
                place.visited && avg != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StarRating(avg.roundToInt(), size = 14.dp)
                    Text("%.1f".format(avg), style = MaterialTheme.typography.labelMedium, color = t.textMuted)
                }
                place.visited -> Text(
                    "Fueron" + (place.visitedAt?.let { " el ${Dates.format(it)}" } ?: "") + " · sin reseñas",
                    style = MaterialTheme.typography.labelMedium,
                    color = t.visitedText,
                )
                place.plannedDate != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Event, null, tint = t.primaryText, modifier = Modifier.size(14.dp))
                    Text(
                        "${Dates.format(place.plannedDate)} (${Dates.relativeDays(place.plannedDate)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = t.primaryText,
                    )
                }
            }
            if (place.tags.isNotEmpty() || (!place.visited && place.interests.isNotEmpty())) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!place.visited && place.interests.isNotEmpty()) {
                        Icon(Icons.Outlined.ThumbUp, null, tint = t.primaryText, modifier = Modifier.size(14.dp))
                        Text("${place.interests.size}", style = MaterialTheme.typography.labelMedium, color = t.primaryText)
                    }
                    place.tags.take(3).forEach { Pill("#$it") }
                }
            }
        }
    }
}

@Composable
private fun Empty(visited: Boolean, hasAny: Boolean, filtering: Boolean, onAdd: () -> Unit) {
    when {
        filtering -> EmptyState(Icons.Outlined.SearchOff, "Nada con ese filtro")
        visited -> EmptyState(Icons.Outlined.Map, "Todavía no fueron a ningún lado", "Cuando vayan a un lugar, entren y toquen “¡Fuimos!”.")
        else -> EmptyState(if (hasAny) Icons.Outlined.Celebration else Icons.Outlined.Place, if (hasAny) "¡Fueron a todos!" else "¿A dónde quieren ir?") {
            TextButton(onClick = onAdd) { Text(if (hasAny) "Agregar otro lugar" else "Agregar el primer lugar") }
        }
    }
}
