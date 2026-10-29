package com.fdm.plancito.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.data.Dates
import com.fdm.plancito.data.Place
import com.fdm.plancito.data.Profile
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.data.summarize
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.EmptyState
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.ScreenHeader
import com.fdm.plancito.ui.common.SectionCard
import com.fdm.plancito.ui.common.StarRating
import com.fdm.plancito.ui.common.icon
import com.fdm.plancito.ui.common.rememberCategories
import com.fdm.plancito.ui.theme.Plancito
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SummaryViewModel : BaseViewModel() {
    val places = MutableStateFlow<List<Place>?>(null)
    val members = MutableStateFlow<Map<String, Profile>>(emptyMap())

    init {
        viewModelScope.launch {
            groups.active.map { it?.id }.distinctUntilChanged().collect { id -> if (id != null) refresh() }
        }
    }

    override suspend fun load() {
        val groupId = groups.active.value?.id ?: return
        places.value = repo.places(groupId)
        members.value = repo.members(groupId).associate { it.userId to it.profile }
    }
}

/** Números del grupo activo: cuánto salieron, lo mejor puntuado, la categoría favorita. */
@Composable
fun SummaryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: SummaryViewModel = viewModel()
    val places by vm.places.collectAsState()
    val members by vm.members.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val group by vm.groups.active.collectAsState()
    val categories = rememberCategories()
    val t = Plancito.colors

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(group?.name?.let { "Resumen de $it" } ?: "Resumen", onBack = onBack)
        LoadingBar(loading)
        val list = places ?: run {
            ErrorText(error, Modifier.padding(16.dp))
            return@Column
        }
        if (list.isEmpty()) {
            EmptyState(Icons.Outlined.Insights, "Todavía no hay nada para resumir", "Agreguen lugares y marquen a dónde fueron.")
            return@Column
        }
        val s = summarize(list, Dates.today().year)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("En ${s.year}", "${s.visitedThisYear}", if (s.visitedThisYear == 1) "lugar visitado" else "lugares visitados", Modifier.weight(1f))
                Stat("En total", "${s.visitedTotal}", "visitados", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Pendientes", "${s.pending}", "por conocer", Modifier.weight(1f))
                Stat("Reseñas", "${s.reviews}", "${s.photos} fotos", Modifier.weight(1f))
            }

            if (s.topRated.isNotEmpty()) {
                SectionCard(title = "Los mejor puntuados") {
                    s.topRated.forEachIndexed { i, p ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpen(p.id) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = t.primaryText)
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            StarRating((p.averageRating ?: 0.0).roundToInt(), size = 14.dp)
                        }
                    }
                }
            }

            s.favoriteCategory?.let { value ->
                val cat = categoryInfo(value, categories)
                SectionCard(title = "Lo que más eligen") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(t.primaryTint), contentAlignment = Alignment.Center) {
                            Icon(cat.icon, null, tint = t.primaryText, modifier = Modifier.size(24.dp))
                        }
                        Text(cat.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            s.topAdder?.let { (userId, count) ->
                SectionCard(title = "Quién agrega más lugares") {
                    Text(
                        "${members[userId]?.displayName ?: "Alguien"}: $count " + if (count == 1) "lugar" else "lugares",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            s.lastVisited?.let { p ->
                SectionCard(title = "La última salida") {
                    Text(
                        p.name + (p.visitedAt?.let { " · ${Dates.format(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { onOpen(p.id) },
                    )
                }
            }
            ErrorText(error)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    val t = Plancito.colors
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(t.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = t.textMuted)
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = t.textMuted)
    }
}
