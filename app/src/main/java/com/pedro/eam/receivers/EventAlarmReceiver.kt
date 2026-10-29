package com.pedro.eam.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pedro.eam.utils.NotificationHelper

class EventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("eam_prefs", Context.MODE_PRIVATE)
        val globalEnabled = prefs.getBoolean("global_notifications_enabled", true)
        if (!globalEnabled) return

        val eventId = intent.getStringExtra("event_id") ?: return
        val title = intent.getStringExtra("event_title") ?: "Lembrete Acadêmico"
        val desc = intent.getStringExtra("event_desc") ?: "Você tem um compromisso próximo."

        NotificationHelper.showEventNotification(context, eventId, "Lembrete: $title", desc)
    }
}
