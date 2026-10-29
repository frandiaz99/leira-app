package com.fdm.plancito.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.data.Place
import com.fdm.plancito.data.reminderTime
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Recordatorios locales de la fecha planeada (sin servidor: los agenda WorkManager en el teléfono). */
object Reminders {
    private fun workName(placeId: String) = "reminder-$placeId"

    /** Agenda (o cancela) el recordatorio de cada lugar según su fecha planeada. */
    fun sync(context: Context, places: List<Place>, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        val now = ZonedDateTime.now()
        places.forEach { p ->
            val time = if (enabled && !p.visited && p.plannedDate != null) reminderTime(p.plannedDate, now) else null
            if (time == null) {
                work.cancelUniqueWork(workName(p.id))
            } else {
                val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(Duration.between(now, time.at).toMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            "place_id" to p.id,
                            "group_id" to p.groupId,
                            "planned_date" to p.plannedDate,
                            "title" to (if (time.today) "Hoy: " else "Mañana: ") + p.name,
                        ),
                    )
                    .build()
                work.enqueueUniqueWork(workName(p.id), ExistingWorkPolicy.REPLACE, request)
            }
        }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val placeId = inputData.getString("place_id") ?: return Result.success()
        val app = PlancitoApp.instance
        if (!app.prefs.reminders) return Result.success()
        if (!app.repository.isConfigured) return Result.success()
        // Si mientras tanto lo borraron, lo marcaron como visitado, le cambiaron la fecha o se
        // cerró la sesión, no se avisa. Sin internet no se puede saber: se avisa igual.
        val lookup = runCatching { if (app.repository.awaitSession()) app.repository.place(placeId) else null }
        val place = lookup.getOrNull()
        if (lookup.isSuccess && place == null) return Result.success()
        if (place != null && (place.visited || place.plannedDate != inputData.getString("planned_date"))) return Result.success()
        Notifier.show(
            applicationContext,
            Notifier.REMINDERS,
            placeId.hashCode(),
            inputData.getString("title") ?: "Tienen un plan",
            "Lo anotaron en Plancito. Tocá para ver el lugar.",
            placeId,
            inputData.getString("group_id"),
        )
        return Result.success()
    }
}
