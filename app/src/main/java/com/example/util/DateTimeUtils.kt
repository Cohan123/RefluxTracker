package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.GERMAN)
    private val dateFormatter = SimpleDateFormat("EEEE, d. MMMM", Locale.GERMAN)
    private val shortDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

    fun formatTime(timestamp: Long): String {
        return timeFormatter.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormatter.format(Date(timestamp))
    }

    fun getStartOfDay(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartAndEndOfDay(offsetDays: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return Pair(getStartOfDay(cal), getEndOfDay(cal))
    }

    fun calculateDaysBetween(startMs: Long, endMs: Long): Int {
        val diff = endMs - startMs
        if (diff <= 0) return 1
        return (diff / (24 * 60 * 60 * 1000L)).toInt() + 1
    }
}
