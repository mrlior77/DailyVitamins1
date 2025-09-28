package com.simha.dailyvitamins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // מתזמן שוב את ההתראות אחרי boot
        ReminderReceiver.scheduleDaily(context)
    }
}
