package me.retrodaredevil.solarthing.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.retrodaredevil.solarthing.android.service.restartService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(context != null) {
            println("Received on boot!")
            restartService(context)
        }
    }

}