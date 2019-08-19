package me.retrodaredevil.solarthing.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import me.retrodaredevil.solarthing.android.prefs.Prefs
import me.retrodaredevil.solarthing.android.service.startServiceIfNotRunning

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(context != null && intent != null) {
            if(intent.action!!.endsWith(".BOOT_COMPLETED")){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    println("Got boot completed. We should have got lock boot completed earlier.")
                    return
                }
            }
            println("Received on boot!")
            val prefs = Prefs(context)
            if(prefs.startOnBoot) {
                println("Starting on boot")
                startServiceIfNotRunning(context)
            } else {
                println("Start on boot not enabled!")
            }
        }
    }

}