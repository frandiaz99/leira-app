package com.fdm.plancito.notify

import android.content.Context
import com.fdm.plancito.BuildConfig
import com.fdm.plancito.PlancitoApp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Avisos del grupo con Firebase Cloud Messaging. La configuración sale de google-services.json
 * (ver app/build.gradle.kts); sin ella todo esto no hace nada.
 */
object Push {
    val isConfigured: Boolean get() = BuildConfig.FIREBASE_APP_ID.isNotBlank()

    /** Inicializa Firebase a mano (no se usa el plugin de google-services). */
    fun init(context: Context) {
        if (!isConfigured || FirebaseApp.getApps(context).isNotEmpty()) return
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build(),
        )
    }

    private suspend fun token(): String? {
        if (!isConfigured) return null
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }

    /** Guarda el token de este teléfono para recibir avisos (si el usuario los quiere). */
    suspend fun register() {
        val app = PlancitoApp.instance
        if (!app.prefs.groupNotifications) return
        runCatching { token()?.let { app.repository.saveDeviceToken(it) } }
    }

    /** Deja de recibir avisos en este teléfono (al apagarlos o al cerrar sesión). */
    suspend fun unregister() {
        runCatching { token()?.let { PlancitoApp.instance.repository.deleteDeviceToken(it) } }
    }
}

/** Recibe los avisos que manda la Edge Function "notify" (mensajes de datos). */
class PlancitoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val app = PlancitoApp.instance
        if (!app.prefs.groupNotifications || !app.repository.isConfigured) return
        app.appScope.launch {
            runCatching { if (app.repository.awaitSession()) app.repository.saveDeviceToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!PlancitoApp.instance.prefs.groupNotifications) return
        val data = message.data
        val title = data["title"] ?: return
        Notifier.show(
            this,
            Notifier.GROUP,
            (data["place_id"] ?: title).hashCode() + 1,
            title,
            data["body"].orEmpty(),
            data["place_id"],
            data["group_id"],
        )
    }
}
