package com.example.reminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * AlarmManager alarms are lost on reboot/app update. The boot receiver must
 * restore an enabled reminder from persisted settings and must not schedule
 * anything when the reminder is disabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderBootReceiverTest {

    private lateinit var context: Context
    private lateinit var settings: ReminderSettings
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReminderSettings.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        settings = ReminderSettings(context)
        alarmManager = context.getSystemService(AlarmManager::class.java)
        ShadowAlarmManager.reset()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(ReminderSettings.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        ShadowAlarmManager.reset()
    }

    private fun nextAlarm() = Shadows.shadowOf(alarmManager).peekNextScheduledAlarm()

    @Test
    fun `enabled reminder is rescheduled after boot`() {
        settings.enable(hourOfDay = 7, minute = 45)

        ReminderBootReceiver().onReceive(
            context,
            Intent(Intent.ACTION_BOOT_COMPLETED)
        )

        assertNotNull("Boot must restore the daily alarm", nextAlarm())
    }

    @Test
    fun `enabled reminder is rescheduled after an app update`() {
        settings.enable(hourOfDay = 21, minute = 15)

        ReminderBootReceiver().onReceive(
            context,
            Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
        )

        assertNotNull("App update must restore the daily alarm", nextAlarm())
    }

    @Test
    fun `disabled reminder is not scheduled after boot`() {
        settings.disable()

        ReminderBootReceiver().onReceive(
            context,
            Intent(Intent.ACTION_BOOT_COMPLETED)
        )

        assertNull("Disabled reminder must stay cancelled", nextAlarm())
    }

    @Test
    fun `unrelated broadcasts are ignored`() {
        settings.enable(hourOfDay = 9, minute = 0)

        ReminderBootReceiver().onReceive(
            context,
            Intent(Intent.ACTION_TIME_CHANGED)
        )

        assertNull("Only boot/update broadcasts may schedule alarms", nextAlarm())
    }
}
