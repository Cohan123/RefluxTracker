package com.example.util

import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DateTimeUtilsLocalDayTest {

    private lateinit var previousZone: TimeZone

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    @Test
    fun `local day offset handles today yesterday and tomorrow`() {
        val now = calendar(2026, Calendar.NOVEMBER, 19, 15, 30)
        assertEquals(0, DateTimeUtils.localDayOffset(now, now))
        assertEquals(0, DateTimeUtils.localDayOffset(calendar(2026, Calendar.NOVEMBER, 19, 0, 0), now))
        assertEquals(0, DateTimeUtils.localDayOffset(calendar(2026, Calendar.NOVEMBER, 19, 23, 59), now))
        assertEquals(-1, DateTimeUtils.localDayOffset(calendar(2026, Calendar.NOVEMBER, 18, 23, 59), now))
        assertEquals(-3, DateTimeUtils.localDayOffset(calendar(2026, Calendar.NOVEMBER, 16, 12, 0), now))
        assertEquals(1, DateTimeUtils.localDayOffset(calendar(2026, Calendar.NOVEMBER, 20, 0, 1), now))
    }

    @Test
    fun `offset crosses a DST change correctly`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        // DST starts on 2026-03-29 in Europe/Berlin.
        val afterChange = calendar(2026, Calendar.MARCH, 29, 12, 0)
        val beforeChange = calendar(2026, Calendar.MARCH, 28, 12, 0)
        assertEquals(-1, DateTimeUtils.localDayOffset(beforeChange, afterChange))
    }

    @Test
    fun `minutes of day and start of local day use local midnight`() {
        val timestamp = calendar(2026, Calendar.NOVEMBER, 19, 7, 45)
        assertEquals(7 * 60 + 45, DateTimeUtils.minutesOfDay(timestamp))
        assertEquals(
            calendar(2026, Calendar.NOVEMBER, 19, 0, 0),
            DateTimeUtils.startOfLocalDay(timestamp)
        )
        assertTrue(DateTimeUtils.startOfLocalDay(timestamp) < timestamp)
    }

    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
