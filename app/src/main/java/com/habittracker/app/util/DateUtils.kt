package com.habittracker.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    /** Get today's epoch millis normalized to start of day (00:00 local time). */
    fun todayMillis(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /** Convert epoch millis to LocalDate. */
    fun toLocalDate(millis: Long): LocalDate {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        ).toLocalDate()
    }

    /** Format epoch millis to a readable date string. */
    fun formatDate(millis: Long, pattern: String = "MMM dd, yyyy"): String {
        val date = toLocalDate(millis)
        return date.format(DateTimeFormatter.ofPattern(pattern))
    }

    /** Get the start of day (epoch millis) for a given date. */
    fun startOfDay(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /** Get the start of day (epoch millis) for a LocalDate. */
    fun startOfDay(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /** Get the next day's start (exclusive end for date range queries). */
    fun startOfNextDay(date: LocalDate): Long {
        return date.plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /** Generate list of last N days' start-of-day millis (including today). */
    fun lastNDays(n: Int, endDate: LocalDate = LocalDate.now()): List<Long> {
        return (0 until n).map { daysAgo ->
            startOfDay(endDate.minusDays(daysAgo.toLong()))
        }
    }
}
