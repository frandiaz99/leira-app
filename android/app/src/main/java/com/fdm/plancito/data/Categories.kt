package com.fdm.plancito.data

data class PlaceCategory(val value: String, val label: String, val emoji: String)

/** Mismos valores que el enum place_category de la base. */
val CATEGORIES = listOf(
    PlaceCategory("restaurante", "Restaurante", "🍝"),
    PlaceCategory("bar", "Bar", "🍸"),
    PlaceCategory("cafe", "Café", "☕"),
    PlaceCategory("viaje", "Viaje", "✈️"),
    PlaceCategory("paseo", "Paseo", "🌳"),
    PlaceCategory("experiencia", "Experiencia", "🎟️"),
    PlaceCategory("otro", "Otro", "📍"),
)

fun categoryInfo(value: String): PlaceCategory = CATEGORIES.firstOrNull { it.value == value } ?: CATEGORIES.last()

/** Emojis para elegir al crear un grupo. */
val GROUP_EMOJIS = listOf("💞", "🍻", "🍕", "✈️", "🎉", "🌮", "🏖️", "🎸", "⚽", "🏠", "🐶", "⭐")
