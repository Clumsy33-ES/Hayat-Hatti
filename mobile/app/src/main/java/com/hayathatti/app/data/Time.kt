package com.hayathatti.app.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Time {
    private val locale = Locale.forLanguageTag("tr-TR")
    private val display = DateTimeFormatter.ofPattern("d MMM HH:mm", locale)
    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss", locale)

    /** Backend hem saat dilimli hem saat dilimsiz (UTC) zaman döndürebiliyor. */
    fun parse(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
            .getOrNull()
    }

    fun show(value: String?, zone: ZoneId = ZoneId.systemDefault()): String =
        parse(value)?.atZone(zone)?.format(display) ?: "Bilinmiyor"

    fun clock(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(clock)

    fun nowIso(): String = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString()
}
