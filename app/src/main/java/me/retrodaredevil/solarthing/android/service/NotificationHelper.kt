package me.retrodaredevil.solarthing.android.service

import java.text.DateFormat
import java.util.*

fun getHostString(host: String?): String {
    if(host != null){
        if(host.length <= 15){ // allow a host like 255.255.255.255 (15 characters)
            return "$host "
        }
        return host.substring(0, 15 - 3) + "... "
    }
    return ""
}
fun getTimedOutSummary(host: String?) = getHostString(host) + "time out ${getTimeString()}"
fun getConnectedSummary(host: String?) = getHostString(host) + "success"
fun getFailedSummary(host: String?) = getHostString(host) + "fail at ${getTimeString()}"
fun getTimeString(): String = DateFormat.getTimeInstance().format(Calendar.getInstance().time)
