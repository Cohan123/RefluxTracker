package com.example.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Day counting must follow local calendar boundaries, not fixed 24-hour
 * blocks. The 14-day analysis progress is based on this function, and a DST
 * change must not shift it by a day.
 */
class DateTimeUtilsTest {

    private lateinit var originalTimeZone: TimeZone
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(berlin)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun time(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0
    ): Long = Calendar.getInstance(berlin).apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis

    @Test
    fun `same local day counts as one day`() {
        val start = time(2024, Calendar.MAY, 10, 8, 15)
        val end = time(2024, Calendar.MAY, 10, 22, 45)
        assertEquals(1, DateTimeUtils.calculateDaysBetween(start, end))
    }

    @Test
    fun `next local day counts as two days`() {
        val start = time(2024, Calendar.MAY, 10, 23, 59)
        val end = time(2024, Calendar.MAY, 11, 0, 1)
        assertEquals(2, DateTimeUtils.calculateDaysBetween(start, end))
    }

    @Test
    fun `inclusive 14-day range stays 14 across the spring DST change`() {
        val start = time(2024, Calendar.MARCH, 18)
        val end = time(2024, Calendar.MARCH, 31)
        assertEquals(14, DateTimeUtils.calculateDaysBetween(start, end))

        // This single day is only 23 hours long due to the clock change.
        assertEquals(
            2,
            DateTimeUtils.calculateDaysBetween(
                time(2024, Calendar.MARCH, 30),
                time(2024, Calendar.MARCH, 31)
            )
        )
    }

    @Test
    fun `day counting is also correct across the autumn DST change`() {
        assertEquals(
            2,
            DateTimeUtils.calculateDaysBetween(
                time(2024, Calendar.OCTOBER, 26),
                time(2024, Calendar.OCTOBER, 27)
            )
        )
    }

    @Test
    fun `formatted time and retroactive date use the current time zone`() {
        val timestamp = time(2024, Calendar.MARCH, 31, 14, 5)

        assertEquals("14:05", DateTimeUtils.formatTime(timestamp))
        assertTrue(
            "Retroactive dates must show the year, got: " +
                DateTimeUtils.formatDateWithYear(timestamp),
            DateTimeUtils.formatDateWithYear(timestamp).contains("2024")
        )
    }

    @Test
    fun `range with end before start is treated as one day`() {
        val start = time(2024, Calendar.MAY, 10)
        val end = time(2024, Calendar.MAY, 9)
        assertEquals(1, DateTimeUtils.calculateDaysBetween(start, end))
    }
}
