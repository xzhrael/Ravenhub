package com.ravenhub.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ravenhub.app.data.planner.TodoItem
import org.json.JSONObject

object TodoScheduler {

    fun schedule(context: Context, todo: TodoItem) {
        val due = todo.dueDateTime ?: return
        val offsetMinutes = todo.reminderOffsetMinutes ?: 0
        val triggerTime = due - (offsetMinutes * 60 * 1000L)

        if (triggerTime <= System.currentTimeMillis() || todo.isCompleted) {
            cancel(context, todo.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
            putExtra(TodoAlarmReceiver.KEY_TODO_ID, todo.id)
            putExtra(TodoAlarmReceiver.KEY_TODO_TITLE, todo.title)
            putExtra(TodoAlarmReceiver.KEY_IS_ALARM_ENABLED, todo.isAlarmEnabled)
        }

        val requestCode = todo.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (todo.isAlarmEnabled) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (_: Exception) {}
        }

        saveAlarmMetadata(context, todo.id, todo.title, triggerTime, todo.isAlarmEnabled)
    }

    fun cancel(context: Context, todoId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TodoAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        removeAlarmMetadata(context, todoId)
    }

    private fun saveAlarmMetadata(context: Context, id: String, title: String, triggerTime: Long, isAlarm: Boolean) {
        val prefs = context.getSharedPreferences("scheduled_alarms_meta", Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("triggerTime", triggerTime)
            put("isAlarm", isAlarm)
        }
        prefs.edit().putString(id, json.toString()).apply()
    }

    private fun removeAlarmMetadata(context: Context, id: String) {
        val prefs = context.getSharedPreferences("scheduled_alarms_meta", Context.MODE_PRIVATE)
        prefs.edit().remove(id).apply()
    }

    fun rescheduleAllAlarms(context: Context) {
        val prefs = context.getSharedPreferences("scheduled_alarms_meta", Context.MODE_PRIVATE)
        val all = prefs.all
        val now = System.currentTimeMillis()
        val editor = prefs.edit()

        for ((id, value) in all) {
            if (value is String) {
                try {
                    val json = JSONObject(value)
                    val triggerTime = json.getLong("triggerTime")
                    val title = json.getString("title")
                    val isAlarm = json.optBoolean("isAlarm", false)

                    if (triggerTime > now) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
                            putExtra(TodoAlarmReceiver.KEY_TODO_ID, id)
                            putExtra(TodoAlarmReceiver.KEY_TODO_TITLE, title)
                            putExtra(TodoAlarmReceiver.KEY_IS_ALARM_ENABLED, isAlarm)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            id.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        if (isAlarm) {
                            alarmManager.setAlarmClock(
                                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                                pendingIntent
                            )
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            }
                        }
                    } else {
                        editor.remove(id)
                    }
                } catch (_: Exception) {}
            }
        }
        editor.apply()
    }
}
