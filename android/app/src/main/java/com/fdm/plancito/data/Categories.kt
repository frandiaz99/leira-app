package com.fdm.plancito.data

/** El ícono de cada una está en ui/common/Icons.kt. */
data class PlaceCategory(val value: String, val label: String)

/** Mismos valores que el enum place_category de la base. */
val CATEGORIES = listOf(
    PlaceCategory("restaurante", "Restaurante"),
    PlaceCategory("bar", "Bar"),
    PlaceCategory("cafe", "Café"),
    PlaceCategory("viaje", "Viaje"),
    PlaceCategory("paseo", "Paseo"),
    PlaceCategory("experiencia", "Experiencia"),
    PlaceCategory("otro", "Otro"),
)

fun categoryInfo(value: String): PlaceCategory = CATEGORIES.firstOrNull { it.value == value } ?: CATEGORIES.last()

/** Iniciales de un grupo para su avatar: "Con Sofi" → "CS", "Familia" → "FA". */
fun groupInitials(name: String): String {
    val words = name.split(Regex("\\s+")).map { w -> w.filter { it.isLetterOrDigit() } }.filter { it.isNotEmpty() }
    val initials = when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2)
        else -> "${words[0].first()}${words[1].first()}"
    }
    return initials.uppercase()
}

/** Índice estable de color para un grupo (siempre el mismo para el mismo id). */
fun groupColorIndex(groupId: String, colors: Int): Int = Math.floorMod(groupId.hashCode(), colors)
