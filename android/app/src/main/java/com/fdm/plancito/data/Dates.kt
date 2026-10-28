package com.fdm.plancito.data

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Dates {
    val zone: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    private val format = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-AR"))

    fun today(): LocalDate = LocalDate.now(zone)

    /** "2026-10-12" o un timestamp ("2026-10-12T15:00:00+00:00") → "12 oct 2026". */
    fun format(iso: String): String = format.format(parse(iso))

    fun parse(iso: String): LocalDate =
        if (iso.length == 10) LocalDate.parse(iso) else OffsetDateTime.parse(iso.replace(' ', 'T')).atZoneSameInstant(zone).toLocalDate()

    /** "hoy", "mañana", "en 5 días", "hace 3 días". */
    fun relativeDays(iso: String, today: LocalDate = today()): String {
        val diff = ChronoUnit.DAYS.between(today, parse(iso))
        return when {
            diff == 0L -> "hoy"
            diff == 1L -> "mañana"
            diff == -1L -> "ayer"
            diff > 0 -> "en $diff días"
            else -> "hace ${-diff} días"
        }
    }

    /** Fecha del DatePicker de Material (millis UTC a medianoche) ↔ ISO. */
    fun fromPickerMillis(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toString()

    fun toPickerMillis(iso: String): Long = LocalDate.parse(iso).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}
