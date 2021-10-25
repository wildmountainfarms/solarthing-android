package me.retrodaredevil.solarthing.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import me.retrodaredevil.solarthing.android.service.PersistentService

class BootReceiver : BroadcastReceiver() {
    private var alreadyStarted = false
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action!!
//        Toast.makeText(context, "Received action: $action hashCode: ${hashCode()}", Toast.LENGTH_LONG).show()
        if(action.endsWith(".BOOT_COMPLETED")){
            println("Got boot completed. We should have got lock boot completed earlier.")
            if(alreadyStarted){
                println("Good")
                Toast.makeText(context, "Checking to make sure SolarThing is started.", Toast.LENGTH_LONG).show()
                PersistentService.serviceHelper.startServiceIfNotRunning(context)
                return
            } else {
                println("Even though SDK_INT >= N, this isn't started yet!")
                // This is called every time. I remember writing this code a while ago, but I don't remember why this was important enough for a toast
//                    Toast.makeText(context, "SolarThing: Report this: Unexpected boot alreadyStarted=false", Toast.LENGTH_LONG).show()
            }
        }
        println("Received on boot!")
        val miscProfile = createMiscProfileProvider(context).activeProfile.profile
        if(miscProfile.startOnBoot) {
            println("Starting on boot")
            PersistentService.serviceHelper.startServiceIfNotRunning(context)
            alreadyStarted = true
        } else {
            println("Start on boot not enabled!")
        }
    }

}