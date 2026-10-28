package com.fdm.plancito.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.ui.common.Segment
import com.fdm.plancito.ui.common.SegmentedControl
import com.fdm.plancito.ui.places.GroupSwitcher
import com.fdm.plancito.ui.places.PlacesViewModel
import com.fdm.plancito.ui.theme.Plancito

private enum class Show { All, Pending, Visited }

/** Todos los lugares del grupo activo con ubicación. Rosa = pendiente, verde = visitado. */
@Composable
fun MapScreen(onOpen: (String) -> Unit, onManageGroups: () -> Unit) {
    val vm: PlacesViewModel = viewModel()
    val places by vm.places.collectAsState()
    val t = Plancito.colors
    var show by rememberSaveable { mutableStateOf(Show.All) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    val located = places.orEmpty().filter {
        it.hasLocation && when (show) {
            Show.All -> true
            Show.Pending -> !it.visited
            Show.Visited -> it.visited
        }
    }
    val pins = located.map { p ->
        MapPin(p.id, p.lat!!, p.lng!!, categoryInfo(p.category).emoji, if (p.visited) t.visited else t.primary, p.name)
    }

    Box(Modifier.fillMaxSize()) {
        OsmMap(pins = pins, fitAll = true, onPinClick = { selected = it }, onTap = { _, _ -> selected = null }, modifier = Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(t.surface)) { GroupSwitcher(onManageGroups = onManageGroups) }
            SegmentedControl(
                segments = listOf(Segment(Show.All, "Todos"), Segment(Show.Pending, "Pendientes"), Segment(Show.Visited, "Visitados")),
                selected = show,
                onSelect = { show = it },
                height = 36.dp,
            )
        }

        if (places != null && pins.isEmpty()) {
            Text(
                "Ningún lugar con ubicación todavía. Marcala al agregar o editar un lugar.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(RoundedCornerShape(14.dp)).background(t.surface).padding(16.dp),
            )
        }

        located.firstOrNull { it.id == selected }?.let { p ->
            val cat = categoryInfo(p.category)
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp))
                    .background(t.surface).clickable { onOpen(p.id) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(cat.emoji, style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (p.visited) "Visitado" + (p.averageRating?.let { " · ⭐ %.1f".format(it) } ?: "") else "Pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (p.visited) t.visitedText else t.primaryText,
                    )
                }
                Text("Ver ›", style = MaterialTheme.typography.labelLarge, color = t.primaryText)
            }
        }
    }
}
