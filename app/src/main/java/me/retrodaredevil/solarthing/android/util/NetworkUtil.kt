package me.retrodaredevil.solarthing.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

class SSIDPermissionException : Exception()
class SSIDNotAvailable : Exception()

fun checkLocationFinePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
fun checkLocationBackgroundPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        return checkLocationFinePermission(context)
    }
}

@Throws(SSIDPermissionException::class, SSIDNotAvailable::class)
fun getSSID(context: Context): String?{
    if(!checkLocationBackgroundPermission(context)){
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

