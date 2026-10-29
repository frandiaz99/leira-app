package com.fdm.plancito.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Lo que llega al compartir algo con Plancito desde otra app (Google Maps, Instagram, el navegador…). */
data class SharedPlace(
    val name: String,
    val link: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

object Share {
    private val URL_RE = Regex("""https?://\S+""")

    /**
     * Arma un lugar a partir del texto compartido. Google Maps manda
     * "Nombre\nDirección\nhttps://maps.app.goo.gl/…"; Instagram y el navegador, solo el link
     * (y a veces el título en [subject]).
     */
    fun parse(text: String?, subject: String? = null): SharedPlace? {
        val body = text?.trim().orEmpty()
        val link = URL_RE.find(body)?.value?.trimEnd('.', ',', ')', ']')
        val lines = (if (link != null) body.replace(link, "") else body)
            .lines().map { it.trim().trim('"', '“', '”') }.filter { it.isNotEmpty() }
        val title = subject?.trim()?.takeIf { it.isNotEmpty() && !URL_RE.containsMatchIn(it) }
        val name = title ?: lines.firstOrNull() ?: ""
        if (name.isEmpty() && link == null) return null
        val address = if (title == null) lines.getOrNull(1) else lines.firstOrNull()?.takeIf { it != title }
        val coords = link?.let { coordinatesIn(it) }
        return SharedPlace(name.take(120), link, address, coords?.first, coords?.second)
    }

    /** Coordenadas dentro de un link de Google Maps (el del lugar, no el centro de la vista, si están los dos). */
    fun coordinatesIn(url: String): Pair<Double, Double>? {
        val patterns = listOf(
            Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)"""),
            Regex("""[?&](?:q|ll|query|center|destination)=(-?\d+\.\d+)(?:,|%2C)(?:\+|%20)?(-?\d+\.\d+)"""),
            Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)"""),
        )
        for (re in patterns) {
            val m = re.find(url) ?: continue
            val lat = m.groupValues[1].toDoubleOrNull() ?: continue
            val lng = m.groupValues[2].toDoubleOrNull() ?: continue
            if (lat in -90.0..90.0 && lng in -180.0..180.0) return lat to lng
        }
        return null
    }

    /** Links cortos de Google Maps: hay que seguir la redirección para ver las coordenadas. */
    fun isShortMapsLink(url: String): Boolean = url.contains("maps.app.goo.gl") || url.contains("goo.gl/maps")

    /** Sigue las redirecciones (hasta 5) y devuelve la URL final, o null si falla. */
    suspend fun resolve(url: String): String? = withContext(Dispatchers.IO) {
        var current = url
        repeat(5) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            try {
                val location = conn.getHeaderField("Location")
                if (conn.responseCode !in 300..399 || location == null) return@withContext current
                current = URL(URL(current), location).toString()
            } catch (_: Exception) {
                return@withContext null
            } finally {
                conn.disconnect()
            }
        }
        current
    }
}
