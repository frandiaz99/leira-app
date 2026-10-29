package com.fdm.plancito.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fdm.plancito.R
import com.fdm.plancito.ui.MainActivity

/** Notificaciones del teléfono: recordatorios de fechas y avisos del grupo. */
object Notifier {
    const val REMINDERS = "recordatorios"
    const val GROUP = "grupo"

    /** Extras del intent que abre un lugar al tocar la notificación. */
    const val EXTRA_PLACE_ID = "place_id"
    const val EXTRA_GROUP_ID = "group_id"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(REMINDERS, "Recordatorios de fechas", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "El día anterior a la fecha que anotaron para ir a un lugar." },
        )
        manager.createNotificationChannel(
            NotificationChannel(GROUP, "Novedades del grupo", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Cuando alguien del grupo agrega un lugar o deja una reseña." },
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, channel: String, id: Int, title: String, text: String, placeId: String?, groupId: String?) {
        if (!canNotify(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PLACE_ID, placeId)
            putExtra(EXTRA_GROUP_ID, groupId)
        }
        val pending = PendingIntent.getActivity(
            context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Sacaron el permiso justo ahora: no pasa nada.
        }
    }
}
