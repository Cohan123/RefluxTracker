package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Receives the alarm fired by [ReminderScheduler] and posts the reminder
 * notification. Tapping the notification opens [MainActivity].
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        ReminderScheduler.ensureChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(com.dp.refluxtracker.R.drawable.ic_notification)
            .setContentTitle(context.getString(com.dp.refluxtracker.R.string.reminder_title))
            .setContentText(context.getString(com.dp.refluxtracker.R.string.reminder_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.notify(ReminderScheduler.NOTIFICATION_ID, n)
    }

    companion object {
        const val ACTION = "com.example.refluxtracker.ACTION_DAILY_REMINDER"
    }
}
