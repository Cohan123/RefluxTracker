package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-creates the optional daily reminder after a reboot or app update.
 *
 * `AlarmManager` alarms are cleared when the device reboots and can also be
 * lost during an app update. The user's enabled/time settings are persisted
 * in [ReminderSettings], so the alarm can be restored without user interaction.
 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val settings = ReminderSettings(context)
        if (!settings.isEnabled()) return
        val time = settings.getReminderTime() ?: return
        ReminderScheduler.schedule(context, time.hourOfDay, time.minute)
    }
}
