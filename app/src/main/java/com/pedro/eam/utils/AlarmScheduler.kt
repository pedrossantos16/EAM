package com.pedro.eam.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pedro.eam.receivers.EventAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    @SuppressLint("NewApi")
    fun scheduleEventReminder(context: Context, eventId: String, title: String, description: String, eventDateTime: LocalDateTime, minutesBefore: Int) {
        val triggerTime = eventDateTime.minusMinutes(minutesBefore.toLong())
        val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("event_title", title)
            putExtra("event_desc", description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } catch (_: Exception) {
        }
    }

    fun cancelEventReminder(context: Context, eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
