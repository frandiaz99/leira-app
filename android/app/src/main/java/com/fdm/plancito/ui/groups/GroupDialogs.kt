package com.fdm.plancito.ui.groups

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdm.plancito.BuildConfig
import com.fdm.plancito.data.GROUP_EMOJIS
import com.fdm.plancito.data.Group
import com.fdm.plancito.data.Invite
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.theme.Plancito

/** Nombre + emoji. Sirve para crear un grupo y para editarlo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupFormDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    initialEmoji: String = GROUP_EMOJIS.first(),
    error: String? = null,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GROUP_EMOJIS.forEach { e ->
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (e == emoji) Plancito.colors.primaryTint else Color.Transparent)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 22.sp) }
                    }
                }
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim(), emoji) }) { Text(confirmLabel) }
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
