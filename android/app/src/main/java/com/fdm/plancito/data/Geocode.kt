package com.fdm.plancito.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeoResult(val label: String, val lat: Double, val lng: Double)

/**
 * Geocoding gratis con Nominatim (OpenStreetMap). Política de uso: máx. ~1
 * request/seg y un User-Agent que identifique la app; por eso la búsqueda va con debounce.
 */
object Geocode {
    private const val BASE = "https://nominatim.openstreetmap.org"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Item(val display_name: String = "", val lat: String = "", val lon: String = "")

    suspend fun search(query: String): List<GeoResult> {
        val body = get("$BASE/search?format=jsonv2&limit=5&accept-language=es&q=${enc(query)}") ?: return emptyList()
        return json.decodeFromString<List<Item>>(body).mapNotNull { it.toResult() }
    }

    suspend fun reverse(lat: Double, lng: Double): String? {
        val body = get("$BASE/reverse?format=jsonv2&accept-language=es&lat=$lat&lon=$lng") ?: return null
        return runCatching { json.decodeFromString<Item>(body).display_name }.getOrNull()?.ifBlank { null }
    }

    /** Abre la ubicación en Google Maps (o la app de mapas que haya). */
    fun mapsUrl(place: Place): String {
        val q = if (place.lat != null && place.lng != null) "${place.lat},${place.lng}" else place.address ?: place.name
        return "https://www.google.com/maps/search/?api=1&query=${enc(q)}"
    }

    private fun Item.toResult(): GeoResult? {
        val la = lat.toDoubleOrNull() ?: return null
        val lo = lon.toDoubleOrNull() ?: return null
        return GeoResult(display_name, la, lo)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.setRequestProperty("User-Agent", "Plancito Android (com.fdm.plancito)")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
