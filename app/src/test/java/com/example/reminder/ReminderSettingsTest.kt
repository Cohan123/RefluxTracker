package com.example.reminder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behaviour spec for [ReminderSettings].
 *
 * Contract:
 *  - Reminder is disabled by default (first install)
 *  - Enabling + setting a time persists across "process restart" (same Context)
 *  - Disabling clears the stored time but leaves the prefs key in place
 *  - Two settings objects pointing at the same Context see the same state
 *
 * The actual AlarmManager scheduling (see [ReminderScheduler]) is exercised
 * separately and not unit-tested here because AlarmManager is global process
 * state — Robolectric supports it but the tests would be brittle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSettingsTest {

    private lateinit var context: Context
    private lateinit var settings: ReminderSettings

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReminderSettings.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        settings = ReminderSettings(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(ReminderSettings.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `isEnabled is false by default on a fresh install`() {
        assertFalse("Reminder must default to disabled", settings.isEnabled())
    }

    @Test
    fun `getReminderTime returns null when no time is set`() {
        assertNull(settings.getReminderTime())
    }

    @Test
    fun `enable sets the enabled flag and persists the time`() {
        settings.enable(hourOfDay = 19, minute = 30)
        assertTrue("Reminder must report enabled after enable()", settings.isEnabled())
        assertNotNull("Reminder must report a time after enable()", settings.getReminderTime())
        val time = settings.getReminderTime()!!
        assertEquals(19, time.hourOfDay)
        assertEquals(30, time.minute)
    }

    @Test
    fun `disable clears the stored time and the enabled flag`() {
        settings.enable(8, 0)
        settings.disable()
        assertFalse(settings.isEnabled())
        assertNull(settings.getReminderTime())
    }

    @Test
    fun `a second ReminderSettings over the same Context sees the persisted state`() {
        settings.enable(21, 15)
        val other = ReminderSettings(context)
        assertTrue(other.isEnabled())
        assertEquals(21, other.getReminderTime()!!.hourOfDay)
        assertEquals(15, other.getReminderTime()!!.minute)
    }

    @Test
    fun `setting a new time while enabled overwrites the previous time`() {
        settings.enable(8, 0)
        settings.enable(20, 45)
        val time = settings.getReminderTime()!!
        assertEquals(20, time.hourOfDay)
        assertEquals(45, time.minute)
    }
}
