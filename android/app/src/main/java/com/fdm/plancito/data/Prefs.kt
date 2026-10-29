package com.fdm.plancito.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferencias locales del teléfono (no se sincronizan). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("plancito", Context.MODE_PRIVATE)

    var lastEmail: String
        get() = sp.getString("last_email", "") ?: ""
        set(v) = sp.edit().putString("last_email", v).apply()

    /** Recordatorio el día anterior a la fecha planeada. */
    var reminders: Boolean
        get() = sp.getBoolean("reminders", true)
        set(v) = sp.edit().putBoolean("reminders", v).apply()

    /** Avisos cuando otro miembro agrega un lugar o deja una reseña. */
    var groupNotifications: Boolean
        get() = sp.getBoolean("group_notifications", true)
        set(v) = sp.edit().putBoolean("group_notifications", v).apply()

    /** Ya se pidió el permiso de notificaciones una vez (no insistir). */
    var askedNotifications: Boolean
        get() = sp.getBoolean("asked_notifications", false)
        set(v) = sp.edit().putBoolean("asked_notifications", v).apply()

    private val activeGroup = MutableStateFlow(sp.getString("active_group", null))

    /** Grupo que se ve en Lugares y Mapa. */
    val activeGroupFlow: StateFlow<String?> = activeGroup.asStateFlow()

    fun setActiveGroup(id: String?) {
        sp.edit().putString("active_group", id).apply()
        activeGroup.value = id
    }

    private val pendingInvite = MutableStateFlow(sp.getString("pending_invite", null))

    /**
     * Código de un link de invitación que todavía no se procesó. Se guarda para
     * no perderlo si hay que iniciar sesión primero.
     */
    val pendingInviteFlow: StateFlow<String?> = pendingInvite.asStateFlow()

    fun setPendingInvite(code: String?) {
        sp.edit().putString("pending_invite", code).apply()
        pendingInvite.value = code
    }
}
