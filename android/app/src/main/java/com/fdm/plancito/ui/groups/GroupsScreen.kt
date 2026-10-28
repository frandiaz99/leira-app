package com.fdm.plancito.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.data.Group
import com.fdm.plancito.data.Membership
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.EmptyState
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.Pill
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.common.ScreenHeader
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.flow.MutableStateFlow

class GroupsViewModel : BaseViewModel() {
    /** Grupo recién creado o al que se unió: la pantalla lo abre. */
    val joined = MutableStateFlow<Group?>(null)

    override suspend fun load() = groups.refresh()

    fun create(name: String, emoji: String) = launchSafe {
        val g = repo.createGroup(name, emoji)
        groups.refresh()
        groups.select(g.id)
        joined.value = g
    }

    fun join(code: String) = launchSafe {
        val g = repo.joinGroup(code)
        groups.refresh()
        groups.select(g.id)
        joined.value = g
    }
}

/**
 * Lista de grupos. Sin grupos funciona como bienvenida: crear uno o unirse con código.
 * [onJoined] se llama al crear o unirse (para ir a sus lugares).
 */
@Composable
fun GroupsScreen(onOpen: (String) -> Unit, onJoined: () -> Unit) {
    val vm: GroupsViewModel = viewModel()
    val memberships by vm.groups.groups.collectAsState()
    val active by vm.groups.active.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val joined by vm.joined.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var joining by remember { mutableStateOf(false) }

    LaunchedEffect(joined) {
        if (joined != null) {
            creating = false
            joining = false
            vm.joined.value = null
            onJoined()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Grupos")
        LoadingBar(loading)
        val list = memberships.orEmpty()
        if (memberships != null && list.isEmpty()) {
            Welcome(onCreate = { creating = true }, onJoin = { joining = true })
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list, key = { it.groupId }) { m ->
                    GroupRow(m, isActive = m.groupId == active?.id, onClick = { onOpen(m.groupId) })
                }
                item {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Crear otro grupo", onClick = { creating = true })
                        OutlinedButton(onClick = { joining = true }, modifier = Modifier.fillMaxWidth()) { Text("Unirme con código") }
                    }
                }
            }
        }
        ErrorText(error, Modifier.padding(16.dp))
    }

    if (creating) {
        GroupFormDialog(
            title = "Nuevo grupo",
            confirmLabel = "Crear",
            error = error,
            onConfirm = { name, emoji -> vm.create(name, emoji) },
            onDismiss = { creating = false; vm.error.value = null },
        )
    }
    if (joining) {
        JoinCodeDialog(error = error, onJoin = vm::join, onDismiss = { joining = false; vm.error.value = null })
    }
}

@Composable
private fun Welcome(onCreate: () -> Unit, onJoin: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EmptyState(
            "👋",
            "Armá tu primer grupo",
            "Un grupo es con quién comparten la lista: tu pareja, amigos, la familia. Podés estar en varios.",
        )
        PrimaryButton("Crear un grupo", onClick = onCreate)
        OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth()) { Text("Tengo un código de invitación") }
    }
}

@Composable
private fun GroupRow(m: Membership, isActive: Boolean, onClick: () -> Unit) {
    val t = Plancito.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(t.surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(t.primaryTint), contentAlignment = Alignment.Center) {
            Text(m.group.emoji, fontSize = 22.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(m.group.name, style = MaterialTheme.typography.titleMedium)
            if (m.role == "owner") Text("Lo administrás vos", style = MaterialTheme.typography.bodySmall, color = t.textMuted)
        }
        if (isActive) Pill("Activo", background = t.primaryTint, color = t.primaryText)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textMuted)
    }
}
