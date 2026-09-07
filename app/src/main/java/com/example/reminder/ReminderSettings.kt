package com.example.reminder

import android.content.Context
import android.content.SharedPreferences

/**
 * Local persistence for the optional daily-reminder feature.
 *
 * Two values:
 *  - enabled flag (boolean)
 *  - reminder time (hour-of-day 0-23, minute 0-59)
 *
 * Backed by SharedPreferences — the data is tiny, no need for DataStore.
 */
class ReminderSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun getReminderTime(): ReminderTime? {
        if (!prefs.contains(KEY_HOUR)) return null
        val h = prefs.getInt(KEY_HOUR, 0)
        val m = prefs.getInt(KEY_MINUTE, 0)
        return ReminderTime(h, m)
    }

    fun enable(hourOfDay: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_HOUR, hourOfDay)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    fun disable() {
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_HOUR)
            .remove(KEY_MINUTE)
            .apply()
    }

    data class ReminderTime(val hourOfDay: Int, val minute: Int)

    companion object {
        const val PREFS_NAME = "reminder_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
    }
}
