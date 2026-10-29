package com.fdm.plancito

import com.fdm.plancito.data.Dates
import com.fdm.plancito.data.averageRating
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.data.groupColorIndex
import com.fdm.plancito.data.groupInitials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DatesAndRatingTest {
    private val today = LocalDate.parse("2026-10-28")

    @Test
    fun relativeDays() {
        assertEquals("hoy", Dates.relativeDays("2026-10-28", today))
        assertEquals("mañana", Dates.relativeDays("2026-10-29", today))
        assertEquals("ayer", Dates.relativeDays("2026-10-27", today))
        assertEquals("en 5 días", Dates.relativeDays("2026-11-02", today))
        assertEquals("hace 3 días", Dates.relativeDays("2026-10-25", today))
    }

    @Test
    fun timestampsUseArgentinaDay() {
        // 01:30 UTC del 29 es todavía el 28 en Buenos Aires.
        assertEquals(LocalDate.parse("2026-10-28"), Dates.parse("2026-10-29T01:30:00.123456+00:00"))
        assertEquals(LocalDate.parse("2026-10-28"), Dates.parse("2026-10-28"))
    }

    @Test
    fun pickerMillisRoundTrip() {
        assertEquals("2026-10-28", Dates.fromPickerMillis(Dates.toPickerMillis("2026-10-28")))
    }

    @Test
    fun average() {
        assertNull(averageRating(emptyList()))
        assertEquals(4.5, averageRating(listOf(4, 5))!!, 0.001)
    }

    @Test
    fun unknownCategoryFallsBackToOtro() {
        assertEquals("otro", categoryInfo("cualquiera").value)
        assertEquals("Bar", categoryInfo("bar").label)
    }

    @Test
    fun groupInitials() {
        assertEquals("CS", groupInitials("Con Sofi"))
        assertEquals("AD", groupInitials("Amigos del club"))
        assertEquals("FA", groupInitials("familia"))
        assertEquals("Á", groupInitials("á"))
        assertEquals("?", groupInitials("  "))
    }

    @Test
    fun groupColorIsStable() {
        val id = "7d2c1f3a-0000-4000-8000-000000000000"
        assertEquals(groupColorIndex(id, 6), groupColorIndex(id, 6))
        assert(groupColorIndex(id, 6) in 0 until 6)
    }
}
