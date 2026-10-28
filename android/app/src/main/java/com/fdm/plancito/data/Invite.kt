package com.fdm.plancito.data

import java.net.URI

/** Links de invitación a un grupo. */
object Invite {
    private val CODE = Regex("^[A-Z0-9]{4,12}$")

    /**
     * Código de un link de invitación, o null si no es uno:
     *   plancito://join/ABC123  (lo que abre la app)
     *   https://<ref>.supabase.co/functions/v1/invite?c=ABC123  (el que se comparte)
     */
    fun codeFrom(link: String?): String? {
        if (link.isNullOrBlank()) return null
        val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return null
        val raw = when {
            uri.scheme == "plancito" && uri.host == "join" -> uri.path?.trim('/')
            uri.scheme == "https" && uri.path?.endsWith("/invite") == true ->
                uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("c=") }?.removePrefix("c=")
            else -> null
        }
        return normalize(raw)
    }

    /** Código tipeado a mano: sin espacios y en mayúsculas. */
    fun normalize(code: String?): String? = code?.trim()?.uppercase()?.takeIf { CODE.matches(it) }

    fun link(supabaseUrl: String, code: String): String = "${supabaseUrl.trimEnd('/')}/functions/v1/invite?c=$code"

    fun shareText(groupName: String, link: String, code: String): String =
        "¡Sumate a \"$groupName\" en Plancito para anotar los lugares a los que vamos! 📍\n$link\n\nSi no abre, entrá a la app y usá el código $code"
}
