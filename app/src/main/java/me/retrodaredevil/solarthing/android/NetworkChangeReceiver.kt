package me.retrodaredevil.solarthing.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.Log

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context!!
        intent!!
        val action = intent.action
        Log.d("Got action", action)
        WifiManager.WIFI_STATE_CHANGED_ACTION
        when(action){
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val current = manager.connectionInfo
                val id: String? = current.bssid
                println("Id is: $id")
            }
            else -> {
                println("Unknown action: $action")
            }
        }
    }
}