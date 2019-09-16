package me.retrodaredevil.solarthing.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.support.v4.content.ContextCompat

class SSIDPermissionException : Exception()
class SSIDNotAvailable : Exception()

@Throws(SSIDPermissionException::class, SSIDNotAvailable::class)
fun getSSID(context: Context): String?{
    if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
        throw SSIDPermissionException()
    }
    val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val current = manager.connectionInfo ?: return null
    println(current.supplicantState)
    if(current.supplicantState == SupplicantState.COMPLETED){
        val r = current.ssid
        if(r == "<unknown ssid>"){
            throw SSIDNotAvailable()
//            return null
        }
        return r
    }

    return null
}

