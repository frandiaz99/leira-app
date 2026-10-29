package com.fdm.plancito.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.BuildConfig
import com.fdm.plancito.data.Profile
import com.fdm.plancito.ui.common.Avatar
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.ScreenHeader
import com.fdm.plancito.ui.common.SectionCard
import com.fdm.plancito.ui.theme.Plancito
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.notify.Push
import com.fdm.plancito.notify.Reminders
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

class ProfileViewModel : BaseViewModel() {
    val profile = MutableStateFlow<Profile?>(null)

    init {
        refresh()
    }

    override suspend fun load() {
        profile.value = repo.myProfile()
    }

    fun rename(name: String) = launchSafe {
        repo.updateDisplayName(name)
        load()
    }

    fun signOut() = launchSafe {
        // Antes de cerrar la sesión: este teléfono deja de recibir avisos y recordatorios.
        Push.unregister()
        Reminders.cancelAll(PlancitoApp.instance)
        groups.clear()
        repo.signOut()
    }

    fun setReminders(on: Boolean) {
        val app = PlancitoApp.instance
        app.prefs.reminders = on
        // Al prenderlos se agendan la próxima vez que carga la lista de lugares.
        if (!on) Reminders.cancelAll(app)
    }

    fun setGroupNotifications(on: Boolean) {
        PlancitoApp.instance.prefs.groupNotifications = on
        viewModelScope.launch { if (on) Push.register() else Push.unregister() }
    }
}

@Composable
fun ProfileScreen(onSummary: () -> Unit) {
    val vm: ProfileViewModel = viewModel()
    val profile by vm.profile.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Perfil")
        LoadingBar(loading)
        val p = profile ?: run {
            ErrorText(error, Modifier.padding(16.dp))
            return@Column
        }
        var name by remember(p.displayName) { mutableStateOf(p.displayName) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(p.displayName, p.avatarUrl, size = 88.dp)
            Text(vm.repo.userEmail.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Plancito.colors.textMuted)
            SectionCard(title = "Cómo te ven en tus grupos") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { vm.rename(name.trim()) },
                    enabled = name.isNotBlank() && name.trim() != p.displayName,
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Guardar") }
            }
            OutlinedButton(onClick = onSummary, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Insights, null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                Text("Resumen del grupo")
            }
            SectionCard(title = "Notificaciones") {
                val prefs = PlancitoApp.instance.prefs
                var reminders by remember { mutableStateOf(prefs.reminders) }
                var groupNotifications by remember { mutableStateOf(prefs.groupNotifications) }
                SettingSwitch("Recordatorios", "El día anterior a la fecha que anotaron para ir.", reminders) {
                    reminders = it
                    vm.setReminders(it)
                }
                if (Push.isConfigured) {
                    SettingSwitch("Novedades del grupo", "Cuando alguien agrega un lugar o deja una reseña.", groupNotifications) {
                        groupNotifications = it
                        vm.setGroupNotifications(it)
                    }
                }
            }
            ErrorText(error)
            OutlinedButton(onClick = vm::signOut, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión", color = Plancito.colors.danger) }
            Text("Plancito ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = Plancito.colors.textMuted)
        }
    }
}

@Composable
private fun SettingSwitch(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Plancito.colors.textMuted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
