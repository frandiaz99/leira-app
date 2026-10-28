package com.fdm.plancito.ui.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.data.Member
import com.fdm.plancito.ui.common.Avatar
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.ConfirmDialog
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.Pill
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.common.ScreenHeader
import com.fdm.plancito.ui.common.SectionCard
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.flow.MutableStateFlow

class GroupDetailViewModel(private val groupId: String) : BaseViewModel() {
    val members = MutableStateFlow<List<Member>>(emptyList())
    val left = MutableStateFlow(false)

    init {
        refresh()
        observeChanges()
    }

    override suspend fun load() {
        members.value = repo.members(groupId)
    }

    fun update(name: String, emoji: String) = launchSafe {
        repo.updateGroup(groupId, name, emoji)
        groups.refresh()
    }

    fun regenerateCode() = launchSafe {
        repo.regenerateInvite(groupId)
        groups.refresh()
    }

    fun leave() = launchSafe {
        repo.leaveGroup(groupId)
        groups.refresh()
        left.value = true
    }
}

@Composable
fun GroupDetailScreen(groupId: String, onBack: () -> Unit) {
    val vm: GroupDetailViewModel = viewModel(key = "group-$groupId") { GroupDetailViewModel(groupId) }
    val memberships by vm.groups.groups.collectAsState()
    val members by vm.members.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val left by vm.left.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmNewCode by remember { mutableStateOf(false) }

    LaunchedEffect(left) { if (left) onBack() }

    val membership = memberships?.firstOrNull { it.groupId == groupId }
    val group = membership?.group
    val isOwner = membership?.role == "owner"
    val me = vm.repo.userId

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(group?.let { "${it.emoji} ${it.name}" }, onBack = onBack) {
            if (group != null) TextButton(onClick = { editing = true }) { Text("Editar") }
        }
        LoadingBar(loading)
        if (group == null) return@Column

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Invitar") {
                Text(
                    "Mandales el link por WhatsApp. Lo tocan, entran con Google o mail y contraseña, y quedan en el grupo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Plancito.colors.textMuted,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Código", style = MaterialTheme.typography.labelLarge, color = Plancito.colors.textMuted)
                    Text(group.inviteCode, fontFamily = FontFamily.Monospace, fontSize = 22.sp, letterSpacing = 3.sp)
                }
                PrimaryButton("Mandar por WhatsApp", onClick = { shareInviteWhatsApp(context, group) })
                OutlinedButton(onClick = { shareInvite(context, group) }, modifier = Modifier.fillMaxWidth()) { Text("Compartir por otra app") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copy(context, group.inviteCode) }, modifier = Modifier.weight(1f)) { Text("Copiar código") }
                    if (isOwner) {
                        OutlinedButton(onClick = { confirmNewCode = true }, modifier = Modifier.weight(1f)) { Text("Cambiar código") }
                    }
                }
            }

            SectionCard(title = "Miembros (${members.size})") {
                members.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(m.profile.displayName, m.profile.avatarUrl)
                        Text(
                            m.profile.displayName + if (m.userId == me) " (vos)" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (m.role == "owner") Pill("Admin")
                    }
                }
            }

            ErrorText(error)
            OutlinedButton(onClick = { confirmLeave = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Salir del grupo", color = Plancito.colors.danger)
            }
        }

        if (editing) {
            GroupFormDialog(
                title = "Editar grupo",
                confirmLabel = "Guardar",
                initialName = group.name,
                initialEmoji = group.emoji,
                onConfirm = { name, emoji -> vm.update(name, emoji); editing = false },
                onDismiss = { editing = false },
            )
        }
        if (confirmNewCode) {
            ConfirmDialog(
                title = "¿Cambiar el código?",
                text = "El link y el código anteriores dejan de funcionar. Los miembros actuales siguen en el grupo.",
                confirmLabel = "Cambiar",
                danger = false,
                onConfirm = vm::regenerateCode,
                onDismiss = { confirmNewCode = false },
            )
        }
        if (confirmLeave) {
            ConfirmDialog(
                title = "¿Salir de ${group.name}?",
                text = if (members.size <= 1) "Sos el último miembro: se borra el grupo con todos sus lugares, reseñas y fotos."
                else "Dejás de ver sus lugares. Para volver, alguien te tiene que invitar de nuevo.",
                confirmLabel = "Salir",
                onConfirm = vm::leave,
                onDismiss = { confirmLeave = false },
            )
        }
    }
}

private fun copy(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Código de Plancito", code))
    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
}
