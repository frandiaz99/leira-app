package com.fdm.plancito.ui.login

import android.util.Log
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.data.GoogleSignIn
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.common.friendlyError
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.launch

private const val MIN_PASSWORD = 6
private const val TAG = "Login"
private const val GOOGLE_SETUP_ERROR =
    "Google rechazó la app: falta el OAuth client \"Android\" con el SHA-1 de esta firma en Google Cloud (ver docs/setup.md)."

/** Entrar con Google o con mail y contraseña. */
@Composable
fun LoginScreen(pendingInvite: Boolean) {
    val app = PlancitoApp.instance
    val repo = app.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(app.prefs.lastEmail) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun run(block: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try {
                block()
            } catch (e: GetCredentialCancellationException) {
                // Cerró el selector de cuentas: no es un error. Pero Google también "cancela"
                // ("[16] Account reauth failed") cuando el SHA-1 de la app no está registrado.
                Log.w(TAG, "Google sign-in cancelado", e)
                if (e.message?.contains("reauth", true) == true) error = GOOGLE_SETUP_ERROR
            } catch (e: NoCredentialException) {
                Log.w(TAG, "Google sign-in sin cuentas", e)
                error = "Google no ofreció ninguna cuenta. Si el teléfono tiene una cuenta de Google, " +
                    "falta registrar el SHA-1 de la app en Google Cloud."
            } catch (e: Exception) {
                Log.w(TAG, "Login falló", e)
                error = authError(e)
            } finally {
                busy = false
            }
        }
    }

    val canSubmit = email.contains('@') && password.length >= MIN_PASSWORD && (!creating || name.isNotBlank())

    fun submit() {
        if (!canSubmit) return
        run {
            val to = email.trim().lowercase()
            if (creating) repo.signUpWithPassword(to, password, name.trim())
            else repo.signInWithPassword(to, password)
            app.prefs.lastEmail = to
        }
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

        if (creating) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tu nombre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            supportingText = if (creating) {
                { Text("Mínimo $MIN_PASSWORD caracteres") }
            } else null,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(if (creating) "Crear cuenta" else "Entrar", onClick = { submit() }, enabled = canSubmit, loading = busy)
        TextButton(onClick = { creating = !creating; error = null }, enabled = !busy) {
            Text(if (creating) "Ya tengo cuenta" else "No tengo cuenta: crear una")
        }
        ErrorText(error)
    }
}

/** Los mensajes de Supabase Auth vienen en inglés. */
private fun authError(e: Exception): String {
    val msg = e.message.orEmpty()
    return when {
        msg.contains("Invalid login credentials", true) -> "Mail o contraseña incorrectos."
        msg.contains("already registered", true) || msg.contains("already exists", true) ->
            "Ya hay una cuenta con ese mail. Entrá con tu contraseña o con Google."
        msg.contains("Password should", true) || msg.contains("weak", true) -> "La contraseña es muy corta o muy fácil."
        // Errores del canje del token de Google en Supabase: son de configuración.
        msg.contains("provider is not enabled", true) || msg.contains("provider_disabled", true) ->
            "Google no está habilitado en Supabase (Authentication → Providers → Google)."
        msg.contains("audience", true) ->
            "Supabase no acepta este Client ID de Google: cargá el Client ID \"Web\" en Authentication → Providers → Google."
        msg.contains("nonce", true) -> "Falló la verificación de Google (nonce). Probá de nuevo."
        else -> friendlyError(e)
    }
}
