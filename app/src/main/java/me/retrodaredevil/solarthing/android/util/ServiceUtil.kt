package me.retrodaredevil.solarthing.android.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent

fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Int.MAX_VALUE).any { serviceClass.name == it.service.className }
}

class ServiceHelper(
        private val clazz: Class<*>
) {
    fun isServiceRunning(context: Context): Boolean {
        return isServiceRunning(context, clazz)
    }

    fun restartService(context: Context){
        val serviceIntent = Intent(context, clazz)
        context.stopService(serviceIntent)
        context.startForegroundService(serviceIntent)
    }
    fun startServiceIfNotRunning(context: Context): Boolean {
        if(isServiceRunning(context, clazz)){
            return false
        }
        val serviceIntent = Intent(context, clazz)
        context.startForegroundService(serviceIntent)
        return true
    }
    fun stopService(context: Context){
        val serviceIntent = Intent(context, clazz)
        context.stopService(serviceIntent)
    }

}
