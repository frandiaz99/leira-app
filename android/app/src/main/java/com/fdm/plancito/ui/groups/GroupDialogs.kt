package com.fdm.plancito.ui.groups

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.fdm.plancito.BuildConfig
import com.fdm.plancito.data.Group
import com.fdm.plancito.data.Invite
import com.fdm.plancito.ui.common.ErrorText

/** Nombre del grupo. Sirve para crearlo y para editarlo. */
@Composable
fun GroupFormDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    error: String? = null,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej: Con Sofi, Amigos del club…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim()) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Unirse tipeando el código que pasó otro miembro. */
@Composable
fun JoinCodeDialog(error: String?, onJoin: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    val normalized = Invite.normalize(code)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unirme con código") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(12) },
                    label = { Text("Código") },
                    placeholder = { Text("Ej: 4F9A2C") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                ErrorText(error)
            }
        },
        confirmButton = { TextButton(enabled = normalized != null, onClick = { onJoin(normalized!!) }) { Text("Unirme") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun inviteIntent(group: Group): Intent {
    val link = Invite.link(BuildConfig.SUPABASE_URL, group.inviteCode)
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, Invite.shareText(group.name, link, group.inviteCode))
    }
}

/** Menú de compartir de Android: WhatsApp, Telegram, mail, etc. */
fun shareInvite(context: Context, group: Group) {
    context.startActivity(Intent.createChooser(inviteIntent(group), "Invitar a ${group.name}"))
}

/** Abre WhatsApp (o WhatsApp Business) con el link listo; si no hay ninguno, el menú de compartir. */
fun shareInviteWhatsApp(context: Context, group: Group) {
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        try {
            context.startActivity(inviteIntent(group).setPackage(pkg))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
    shareInvite(context, group)
}
