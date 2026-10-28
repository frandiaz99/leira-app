package com.fdm.plancito.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.data.GoogleSignIn
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.common.friendlyError
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.launch

/** Entrar con Google o con un código que llega al mail (sin contraseñas). */
@Composable
fun LoginScreen(pendingInvite: Boolean) {
    val app = PlancitoApp.instance
    val repo = app.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(app.prefs.lastEmail) }
    var code by remember { mutableStateOf("") }
    var codeSentTo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun run(block: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try {
                block()
            } catch (_: GetCredentialCancellationException) {
                // Cerró el selector de cuentas: no es un error.
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                busy = false
            }
        }
    }

    fun sendCode() = run {
        val to = email.trim().lowercase()
        repo.sendEmailCode(to)
        app.prefs.lastEmail = to
        codeSentTo = to
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📍💞", fontSize = 52.sp)
        Text("Plancito", style = MaterialTheme.typography.displaySmall)
        Text(
            if (pendingInvite) "Entrá para sumarte al grupo que te invitó."
            else "Los lugares a los que quieren ir, y lo que les pareció cuando fueron.",
            style = MaterialTheme.typography.bodyLarge,
            color = Plancito.colors.textMuted,
            textAlign = TextAlign.Center,
        )

        if (codeSentTo == null) {
            if (GoogleSignIn.isConfigured) {
                OutlinedButton(
                    onClick = { run { GoogleSignIn.signIn(context, repo) } },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Continuar con Google", style = MaterialTheme.typography.titleMedium) }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.weight(1f), color = Plancito.colors.border)
                    Text("o con tu mail", style = MaterialTheme.typography.labelMedium, color = Plancito.colors.textMuted)
                    HorizontalDivider(Modifier.weight(1f), color = Plancito.colors.border)
                }
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (email.contains('@')) sendCode() }),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Mandarme un código", onClick = { sendCode() }, enabled = email.contains('@'), loading = busy)
        } else {
            Text(
                "Te mandamos un código a $codeSentTo. Puede tardar un minuto; fijate en spam.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) },
                label = { Text("Código de 6 dígitos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (code.length == 6) run { repo.verifyEmailCode(codeSentTo!!, code) } }),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                "Entrar",
                onClick = { run { repo.verifyEmailCode(codeSentTo!!, code) } },
                enabled = code.length == 6,
                loading = busy,
            )
            TextButton(onClick = { codeSentTo = null; code = "" }, enabled = !busy) { Text("Usar otro mail") }
        }
        ErrorText(error)
    }
}
