package me.retrodaredevil.solarthing.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import me.retrodaredevil.solarthing.android.service.startServiceIfNotRunning

class BootReceiver : BroadcastReceiver() {
    private var alreadyStarted = false
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action!!
//        Toast.makeText(context, "Received action: $action hashCode: ${hashCode()}", Toast.LENGTH_LONG).show()
        if(action.endsWith(".BOOT_COMPLETED")){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                println("Got boot completed. We should have got lock boot completed earlier.")
                if(alreadyStarted){
                    println("Good")
                    Toast.makeText(context, "Checking to make sure SolarThing is started.", Toast.LENGTH_LONG).show()
                    startServiceIfNotRunning(context)
                    return
                } else {
                    println("Even though SDK_INT >= N, this isn't started yet!")
                    Toast.makeText(context, "SolarThing: Report this: Unexpected boot alreadyStarted=false", Toast.LENGTH_LONG).show()
                }
            }
        }
        println("Received on boot!")
        val miscProfile = createMiscProfileProvider(context).activeProfile.profile
        if(miscProfile.startOnBoot) {
            println("Starting on boot")
            startServiceIfNotRunning(context)
            alreadyStarted = true
        } else {
            println("Start on boot not enabled!")
        }
    }

}