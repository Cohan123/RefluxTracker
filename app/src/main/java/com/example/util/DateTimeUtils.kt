package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    // SimpleDateFormat is not thread-safe and captures the default time zone
    // when it is created. Keep one formatter per thread and refresh the zone on
    // every call so UI/background callers and runtime time-zone changes are safe.
    // (ThreadLocal.withInitial is API 26+; the anonymous subclass works on 24.)
    private fun formatterThreadLocal(pattern: String): ThreadLocal<SimpleDateFormat> =
        object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat(pattern, Locale.GERMAN)
        }

    private val timeFormatter = formatterThreadLocal("HH:mm")
    private val dateFormatter = formatterThreadLocal("EEEE, d. MMMM")
    private val dateWithYearFormatter = formatterThreadLocal("EEEE, d. MMMM yyyy")
    private val shortDateFormatter = formatterThreadLocal("dd.MM.yyyy")

    private fun SimpleDateFormat.withCurrentTimeZone(): SimpleDateFormat = apply {
        timeZone = TimeZone.getDefault()
    }

    fun formatTime(timestamp: Long): String {
        return timeFormatter.get()!!.withCurrentTimeZone().format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormatter.get()!!.withCurrentTimeZone().format(Date(timestamp))
    }

    fun formatDateWithYear(timestamp: Long): String {
        return dateWithYearFormatter.get()!!.withCurrentTimeZone().format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormatter.get()!!.withCurrentTimeZone().format(Date(timestamp))
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

    /**
     * Signed local calendar-day difference: `-1` means yesterday, `0` today.
     *
     * Iterating the calendar instead of dividing by 24 h keeps the result
     * correct on DST change days. A timestamp on the same local day always
     * yields 0, so a retroactive entry at 23:59 still selects the right day.
     */
    fun localDayOffset(timestamp: Long, now: Long = System.currentTimeMillis()): Int {
        val target = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var offset = 0
        while (target.before(today)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
            offset--
        }
        while (target.after(today)) {
            target.add(Calendar.DAY_OF_YEAR, -1)
            offset++
        }
        return offset
    }

    /** Minutes since local midnight (00:00), used for habit time clustering. */
    fun minutesOfDay(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /** Local midnight of the day containing [timestamp]. */
    fun startOfLocalDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return getStartOfDay(cal)
    }

    /**
     * Inclusive count of local calendar days between two timestamps.
     *
     * Iterating day boundaries instead of dividing by 24 hours keeps the count
     * correct on DST change days (23/25-hour days) and for timestamps around
     * midnight.
     */
    fun calculateDaysBetween(startMs: Long, endMs: Long): Int {
        if (endMs <= startMs) return 1
        val start = Calendar.getInstance().apply {
            timeInMillis = startMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            timeInMillis = endMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var days = 1
        while (start.before(end)) {
            start.add(Calendar.DAY_OF_YEAR, 1)
            days++
        }
        return days
    }
}
