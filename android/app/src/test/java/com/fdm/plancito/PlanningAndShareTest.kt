package com.fdm.plancito

import com.fdm.plancito.data.Interest
import com.fdm.plancito.data.Place
import com.fdm.plancito.data.PlaceOrder
import com.fdm.plancito.data.Review
import com.fdm.plancito.data.Share
import com.fdm.plancito.data.priceLabel
import com.fdm.plancito.data.reminderTime
import com.fdm.plancito.data.sortPlaces
import com.fdm.plancito.data.summarize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PlanningAndShareTest {
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    private fun place(
        id: String,
        name: String = id,
        createdAt: String = "2026-01-01T00:00:00+00:00",
        plannedDate: String? = null,
        visitedAt: String? = null,
        ratings: List<Int> = emptyList(),
        interested: Int = 0,
        price: Int? = null,
        category: String = "otro",
        createdBy: String = "u1",
    ) = Place(
        id = id, groupId = "g", createdBy = createdBy, name = name, category = category,
        plannedDate = plannedDate, status = if (visitedAt != null) Place.STATUS_VISITED else Place.STATUS_WISHLIST,
        visitedAt = visitedAt, price = price, createdAt = createdAt,
        reviews = ratings.mapIndexed { i, r -> Review("r$id$i", id, "u$i", r) },
        interests = (1..interested).map { Interest("u$it") },
    )

    @Test
    fun sharedFromGoogleMaps() {
        val s = Share.parse("La Cabrera\nJosé Antonio Cabrera 5099, C1414 CABA\nhttps://maps.app.goo.gl/AbC123")!!
        assertEquals("La Cabrera", s.name)
        assertEquals("José Antonio Cabrera 5099, C1414 CABA", s.address)
        assertEquals("https://maps.app.goo.gl/AbC123", s.link)
        assertTrue(Share.isShortMapsLink(s.link!!))
        assertNull(s.lat)
    }

    @Test
    fun sharedLinkOnlyUsesSubjectAsName() {
        val s = Share.parse("https://www.instagram.com/p/xyz/", subject = "Café Tortoni")!!
        assertEquals("Café Tortoni", s.name)
        assertEquals("https://www.instagram.com/p/xyz/", s.link)
        assertNull(Share.parse("   "))
    }

    @Test
    fun coordinatesFromMapsUrls() {
        // El lugar (!3d!4d) gana sobre el centro de la vista (@).
        val full = "https://www.google.com/maps/place/La+Cabrera/@-34.58,-58.43,17z/data=!3d-34.5856!4d-58.4321"
        assertEquals(-34.5856 to -58.4321, Share.coordinatesIn(full))
        assertEquals(-34.6 to -58.38, Share.coordinatesIn("https://maps.google.com/?q=-34.6,-58.38"))
        assertEquals(-34.6 to -58.38, Share.coordinatesIn("https://www.google.com/maps/@-34.6,-58.38,15z"))
        assertNull(Share.coordinatesIn("https://www.instagram.com/p/xyz/"))
    }

    @Test
    fun reminderTheDayBefore() {
        val now = ZonedDateTime.of(2026, 10, 29, 12, 0, 0, 0, zone)
        val r = reminderTime("2026-11-02", now)!!
        assertEquals(ZonedDateTime.of(2026, 11, 1, 10, 0, 0, 0, zone), r.at)
        assertFalse(r.today)
        // Si la víspera ya pasó, el mismo día a las 9.
        val tomorrowLate = reminderTime("2026-10-30", ZonedDateTime.of(2026, 10, 29, 22, 0, 0, 0, zone))!!
        assertTrue(tomorrowLate.today)
        // Fecha pasada o de hoy al mediodía: sin aviso.
        assertNull(reminderTime("2026-10-29", now))
        assertNull(reminderTime("2026-10-01", now))
    }

    @Test
    fun sortOrders() {
        val a = place("a", name = "Bar", createdAt = "2026-03-01T00:00:00+00:00", interested = 1, price = 3)
        val b = place("b", name = "alto", createdAt = "2026-02-01T00:00:00+00:00", plannedDate = "2026-11-10", interested = 3)
        val c = place("c", name = "Cine", createdAt = "2026-04-01T00:00:00+00:00", plannedDate = "2026-11-05", price = 1)
        val list = listOf(a, b, c)
        assertEquals(listOf("c", "b", "a"), sortPlaces(list, false).map { it.id })
        assertEquals(listOf("c", "a", "b"), sortPlaces(list, false, PlaceOrder.Recent).map { it.id })
        assertEquals(listOf("b", "a", "c"), sortPlaces(list, false, PlaceOrder.Name).map { it.id })
        assertEquals(listOf("b", "a", "c"), sortPlaces(list, false, PlaceOrder.Interest).map { it.id })
        assertEquals(listOf("c", "a", "b"), sortPlaces(list, false, PlaceOrder.Price).map { it.id })
        val v1 = place("v1", visitedAt = "2026-05-01", ratings = listOf(3))
        val v2 = place("v2", visitedAt = "2026-06-01", ratings = listOf(5, 4))
        val v3 = place("v3", visitedAt = "2026-07-01")
        assertEquals(listOf("v2", "v1", "v3"), sortPlaces(listOf(v1, v2, v3), true, PlaceOrder.Rating).map { it.id })
        assertEquals(listOf("v3", "v2", "v1"), sortPlaces(listOf(v1, v2, v3), true).map { it.id })
    }

    @Test
    fun prices() {
        assertEquals("$$", priceLabel(2))
        assertNull(priceLabel(null))
        assertNull(priceLabel(7))
    }

    @Test
    fun summary() {
        val places = listOf(
            place("a", visitedAt = "2026-03-01", ratings = listOf(5), category = "bar", createdBy = "sofi"),
            place("b", visitedAt = "2025-12-01", ratings = listOf(3), category = "bar", createdBy = "sofi"),
            place("c", visitedAt = "2026-08-15", category = "cafe", createdBy = "fran"),
            place("d", createdBy = "sofi"),
        )
        val s = summarize(places, 2026)
        assertEquals(2, s.visitedThisYear)
        assertEquals(3, s.visitedTotal)
        assertEquals(1, s.pending)
        assertEquals(2, s.reviews)
        assertEquals(listOf("a", "b"), s.topRated.map { it.id })
        assertEquals("bar", s.favoriteCategory)
        assertEquals("sofi" to 3, s.topAdder)
        assertEquals("c", s.lastVisited?.id)
    }
}
