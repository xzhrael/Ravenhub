package com.ravenhub.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ravenhub.app.MainActivity
import com.ravenhub.app.R

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getStringExtra(KEY_TODO_ID) ?: return
        val todoTitle = intent.getStringExtra(KEY_TODO_TITLE) ?: "Task Reminder"
        val isAlarm = intent.getBooleanExtra(KEY_IS_ALARM_ENABLED, false)

        showNotification(context, todoId, todoTitle, isAlarm)
    }

    private fun showNotification(context: Context, todoId: String, title: String, isAlarm: Boolean) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = if (isAlarm) "todo_alarm_clock_channel" else "todo_reminders"
        val alarmSound = RingtoneManager.getDefaultUri(
            if (isAlarm) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (isAlarm) "Task Alarms (Clock Sound)" else "Task Reminders"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isAlarm) "Ringing alarm clock for critical tasks" else "Notifications for task reminders"
                enableVibration(true)
                if (isAlarm) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(alarmSound, audioAttributes)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            todoId.hashCode(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(if (isAlarm) "Task Alarm Clock" else "Task Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(if (isAlarm) NotificationCompat.DEFAULT_VIBRATE else NotificationCompat.DEFAULT_ALL)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isAlarm) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        notificationManager.notify(todoId.hashCode(), builder.build())
    }

    companion object {
        const val KEY_TODO_ID = "extra_todo_id"
        const val KEY_TODO_TITLE = "extra_todo_title"
        const val KEY_IS_ALARM_ENABLED = "extra_is_alarm_enabled"
    }
}
