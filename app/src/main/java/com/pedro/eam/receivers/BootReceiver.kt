package com.pedro.eam.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pedro.eam.data.EamRepository
import com.pedro.eam.utils.AlarmScheduler
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("eam_prefs", Context.MODE_PRIVATE)
            val globalEnabled = prefs.getBoolean("global_notifications_enabled", true)
            if (!globalEnabled) return

            EamRepository.init(context)
            val now = LocalDateTime.now()
            EamRepository.events.filter { !it.isCompleted && it.reminderEnabled }.forEach { ev ->
                if (ev.time != null) {
                    val dt = LocalDateTime.of(ev.date, ev.time)
                    if (dt.isAfter(now)) {
                        AlarmScheduler.scheduleEventReminder(context, ev.id, ev.title, ev.description, dt, ev.reminderMinutesBefore)
                    }
                }
            }
        }
    }
}
