package com.fdm.plancito.data

/**
 * Categoría de un lugar: una fija ([value] = "restaurante", "bar"…) o una propia del grupo
 * ([value] = su id en la tabla categories). [iconName] es uno de [CATEGORY_ICONS]; el dibujo
 * está en ui/common/Icons.kt.
 */
data class PlaceCategory(val value: String, val label: String, val iconName: String, val custom: Boolean = false)

/** Las fijas. "otro" va siempre última. */
val CATEGORIES = listOf(
    PlaceCategory("restaurante", "Restaurante", "restaurant"),
    PlaceCategory("bar", "Bar", "bar"),
    PlaceCategory("cafe", "Café", "cafe"),
    PlaceCategory("viaje", "Viaje", "flight"),
    PlaceCategory("paseo", "Paseo", "park"),
    PlaceCategory("experiencia", "Experiencia", "ticket"),
    PlaceCategory("otro", "Otro", "place"),
)

/** Íconos para elegir al crear una categoría propia. */
val CATEGORY_ICONS = listOf(
    "place", "restaurant", "pizza", "icecream", "cafe", "bar", "nightlife", "music",
    "theater", "movie", "museum", "library", "shopping", "park", "beach", "hiking",
    "sports", "fitness", "flight", "hotel", "ticket", "pets", "star",
)

/** Fijas (sin "otro") + las propias del grupo + "otro". */
fun allCategories(custom: List<PlaceCategory>): List<PlaceCategory> = CATEGORIES.dropLast(1) + custom + CATEGORIES.last()

/** La categoría de un lugar; si no existe (p. ej. la borraron), "otro". */
fun categoryInfo(value: String, custom: List<PlaceCategory> = emptyList()): PlaceCategory =
    CATEGORIES.firstOrNull { it.value == value } ?: custom.firstOrNull { it.value == value } ?: CATEGORIES.last()

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
