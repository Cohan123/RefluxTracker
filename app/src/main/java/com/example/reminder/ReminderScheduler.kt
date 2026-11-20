package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Schedules / cancels the optional daily reminder using [AlarmManager].
 *
 * On Android 12+ (API 31+) scheduling exact alarms requires the
 * `SCHEDULE_EXACT_ALARM` permission OR falling back to
 * `setAndAllowWhileIdle` (inexact but no permission needed). We use the
 * inexact variant — a daily reminder does not need second-precision, and
 * the permission flow would require user opt-in to a system settings page
 * which is friction nobody wants on a personal tracker.
 *
 * The notification channel ID is exposed so the [ReminderReceiver] can post
 * notifications on the same channel.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "reminder_channel"
    const val NOTIFICATION_ID = 1001
    const val REQUEST_CODE = 2001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(com.dp.refluxtracker.R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(com.dp.refluxtracker.R.string.reminder_channel_description)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun schedule(context: Context, hourOfDay: Int, minute: Int) {
        ensureChannel(context)
        val triggerAt = nextTriggerMillis(hourOfDay, minute)
        val pi = pendingIntent(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        // Inexact daily alarm; may drift a few minutes. The boot/update receiver
        // re-creates it after the process loses its alarms.
        am.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(hourOfDay: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
