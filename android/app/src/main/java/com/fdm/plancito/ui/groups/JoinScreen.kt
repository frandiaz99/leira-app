package com.fdm.plancito.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.data.InvitePreview
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.EmptyState
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.flow.MutableStateFlow

class JoinViewModel(private val code: String) : BaseViewModel() {
    val preview = MutableStateFlow<InvitePreview?>(null)
    val notFound = MutableStateFlow(false)
    val done = MutableStateFlow(false)

    init {
        refresh()
    }

    override suspend fun load() {
        val p = repo.invitePreview(code)
        preview.value = p
        notFound.value = p == null
    }

    fun join() = launchSafe {
        val g = repo.joinGroup(code)
        groups.refresh()
        groups.select(g.id)
        done.value = true
    }
}

/** Se abre desde un link de invitación (plancito://join/CODIGO). */
@Composable
fun JoinScreen(code: String, onDone: () -> Unit) {
    val vm: JoinViewModel = viewModel(key = "join-$code") { JoinViewModel(code) }
    val preview by vm.preview.collectAsState()
    val notFound by vm.notFound.collectAsState()
    val done by vm.done.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(done) { if (done) onDone() }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingBar(loading)
        val p = preview
        when {
            notFound -> EmptyState("🤔", "Esa invitación no existe", "Puede que hayan cambiado el código. Pediles uno nuevo.")
            p != null -> {
                Text(p.emoji, fontSize = 56.sp)
                Text(
                    if (p.alreadyMember) "Ya estás en ${p.name}" else "Te invitaron a ${p.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (p.members == 1) "1 miembro" else "${p.members} miembros",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Plancito.colors.textMuted,
                )
                PrimaryButton(if (p.alreadyMember) "Ir al grupo" else "Unirme", onClick = vm::join, loading = loading)
            }
        }
        ErrorText(error)
        TextButton(onClick = onDone) { Text(if (notFound) "Volver" else "Ahora no") }
    }
}
