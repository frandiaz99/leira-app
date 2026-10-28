package com.fdm.plancito.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.fdm.plancito.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/** "Continuar con Google" con Credential Manager; el ID token se canjea en Supabase. */
object GoogleSignIn {

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Abre el selector de cuentas. [context] tiene que ser la Activity. */
    suspend fun signIn(context: Context, repo: Repository) {
        // Google recibe el hash del nonce; Supabase, el original, y verifica que coincidan.
        val rawNonce = UUID.randomUUID().toString()
        val hashed = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).setNonce(hashed).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential

        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Google devolvió una credencial inesperada")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        repo.signInWithGoogle(idToken, rawNonce)
    }
}
