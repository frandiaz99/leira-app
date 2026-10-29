package com.fdm.plancito.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Cuándo avisar de una fecha planeada. [today] = el aviso es del mismo día. */
data class ReminderTime(val at: ZonedDateTime, val today: Boolean)

/**
 * El aviso sale el día anterior a las 10; si eso ya pasó, el mismo día a las 9;
 * si también pasó (o la fecha ya fue), no hay aviso.
 */
fun reminderTime(plannedDate: String, now: ZonedDateTime): ReminderTime? {
    val date = runCatching { LocalDate.parse(plannedDate.take(10)) }.getOrNull() ?: return null
    val dayBefore = date.minusDays(1).atTime(LocalTime.of(10, 0)).atZone(now.zone)
    if (dayBefore.isAfter(now)) return ReminderTime(dayBefore, today = false)
    val sameDay = date.atTime(LocalTime.of(9, 0)).atZone(now.zone)
    if (sameDay.isAfter(now)) return ReminderTime(sameDay, today = true)
    return null
}

/** Cómo ordenar la lista de lugares. */
enum class PlaceOrder(val label: String) {
    Planned("Fecha"),
    Recent("Más nuevos"),
    Name("Nombre"),
    Rating("Mejor puntaje"),
    Interest("Más interesados"),
    Price("Más baratos"),
}

/** Órdenes que tienen sentido en cada pestaña. */
fun ordersFor(visited: Boolean): List<PlaceOrder> =
    if (visited) listOf(PlaceOrder.Planned, PlaceOrder.Rating, PlaceOrder.Name, PlaceOrder.Price)
    else listOf(PlaceOrder.Planned, PlaceOrder.Interest, PlaceOrder.Recent, PlaceOrder.Name, PlaceOrder.Price)

/**
 * [PlaceOrder.Planned]: pendientes con fecha primero (la más cercana arriba) y después el resto
 * por más nuevo; visitados por fecha de visita, el último arriba.
 */
fun sortPlaces(list: List<Place>, visited: Boolean, order: PlaceOrder = PlaceOrder.Planned): List<Place> = when (order) {
    PlaceOrder.Planned ->
        if (visited) list.sortedByDescending { it.visitedAt.orEmpty() }
        else list.sortedWith(compareBy<Place> { it.plannedDate == null }.thenBy { it.plannedDate.orEmpty() }.thenByDescending { it.createdAt })
    PlaceOrder.Recent -> list.sortedByDescending { it.createdAt }
    PlaceOrder.Name -> list.sortedBy { it.name.lowercase() }
    PlaceOrder.Rating -> list.sortedWith(compareBy<Place> { it.averageRating == null }.thenByDescending { it.averageRating ?: 0.0 })
    PlaceOrder.Interest -> list.sortedWith(compareByDescending<Place> { it.interests.size }.thenByDescending { it.createdAt })
    PlaceOrder.Price -> list.sortedWith(compareBy<Place> { it.price == null }.thenBy { it.price ?: 0 })
}

/** "$$" para precio 2. */
fun priceLabel(price: Int?): String? = price?.takeIf { it in 1..4 }?.let { "$".repeat(it) }

/** Resumen del grupo para la pantalla "Resumen". */
data class GroupSummary(
    val year: Int,
    val visitedThisYear: Int,
    val visitedTotal: Int,
    val pending: Int,
    val reviews: Int,
    val photos: Int,
    /** Los 3 mejor puntuados (visitados con al menos una reseña). */
    val topRated: List<Place>,
    /** Valor de la categoría más visitada, si hay alguna visita. */
    val favoriteCategory: String?,
    /** Quién agregó más lugares: (user_id, cantidad). */
    val topAdder: Pair<String, Int>?,
    val lastVisited: Place?,
)

fun summarize(places: List<Place>, year: Int): GroupSummary {
    val visited = places.filter { it.visited }
    return GroupSummary(
        year = year,
        visitedThisYear = visited.count { p -> p.visitedAt?.let { runCatching { Dates.parse(it).year }.getOrNull() } == year },
        visitedTotal = visited.size,
        pending = places.size - visited.size,
        reviews = places.sumOf { it.reviews.size },
        photos = places.sumOf { it.photos.size },
        topRated = visited.filter { it.averageRating != null }
            .sortedWith(compareByDescending<Place> { it.averageRating }.thenByDescending { it.reviews.size })
            .take(3),
        favoriteCategory = visited.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key,
        topAdder = places.groupingBy { it.createdBy }.eachCount().maxByOrNull { it.value }?.toPair(),
        lastVisited = visited.maxByOrNull { it.visitedAt.orEmpty() },
    )
}
