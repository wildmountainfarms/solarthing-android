package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import me.retrodaredevil.solarthing.android.request.DataRequest
import java.text.DateFormat
import java.util.*

fun getHostString(host: String?): String {
    if(host != null){
        if(host.length <= 15){ // allow a host like 255.255.255.255 (15 characters)
            return "$host "
        }
        val sub = host.substring(0, 15 - 3)
        if(sub.endsWith('.')){
            return "$sub.. "
        }
        return "$sub... "
    }
    return ""
}
fun getTimedOutSummary(host: String?) = getHostString(host) + "time out ${getTimeString()}"
fun getConnectedSummary(host: String?) = getHostString(host) + "success"
fun getConnectedNoNewDataSummary(host: String? ) = getHostString(host) + "no new data"
fun getFailedSummary(host: String?) = getHostString(host) + "fail at ${getTimeString()}"
fun getTimeString(): String = DateFormat.getTimeInstance().format(Calendar.getInstance().time)

fun Notification.Builder.failedNotification(request: DataRequest): Notification.Builder {
    if(request.successful){
        throw IllegalArgumentException("Use this method when request.successful == false! It equals true right now!")
    }
    var bigText = ""
    if(request.authDebug != null){
        bigText += request.authDebug + "\n"
    }
    bigText += "Stack trace:\n${request.stackTrace}"

    return setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentTitle("Failed to load data. Will try again.")
        .setContentText(request.simpleStatus)
        .setSubText(getFailedSummary(request.host))
        .setStyle(Notification.BigTextStyle().bigText(bigText))
}
fun Notification.Builder.noDataNotification(request: DataRequest): Notification.Builder {
    return setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentText("Connection successful, but no data.")
        .setSubText(getConnectedSummary(request.host))
}
fun Notification.Builder.timedOutNotification(): Notification.Builder {
    return setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentText("Last request timed out. Will try again.")
        .setSubText(getTimedOutSummary(null))
}
fun Notification.Builder.loadingNotification(): Notification.Builder {
    return setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentText("Loading Data")
        .setSubText("started loading at ${getTimeString()}")
        .setProgress(2, 1, true)
}
fun Context.getManager() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
