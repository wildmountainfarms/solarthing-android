package me.retrodaredevil.solarthing.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationClearedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(context != null) {
            val serviceIntent = Intent(context, PersistentService::class.java)
            context.stopService(serviceIntent)
        }
    }

}