package me.retrodaredevil.solarthing.android

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import me.retrodaredevil.solarthing.android.notifications.NotificationChannelGroups
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels

class StartupHelper(
        private val context: Context
) {
    fun onStartup() {

        val notificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for(channel in NotificationChannels.OLD_CHANNELS) {
                notificationManager.deleteNotificationChannel(channel)
            }

            for(channelGroup in NotificationChannelGroups.values()){
                notificationManager.createNotificationChannelGroup(
                        NotificationChannelGroup(
                                channelGroup.id, channelGroup.getName(context)
                        ).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                description = channelGroup.getDescription(context)
                            }
                        })
            }
            for(notificationChannel in NotificationChannels.values()){
                notificationManager.createNotificationChannel(
                        NotificationChannel(
                                notificationChannel.id,
                                notificationChannel.getName(context),
                                notificationChannel.importance
                        ).apply {
                            description = notificationChannel.getDescription(context)
                            enableLights(notificationChannel.enableLights)
                            if (notificationChannel.lightColor != null) {
                                lightColor = notificationChannel.lightColor
                            }
                            enableVibration(notificationChannel.enableVibration)
                            if (notificationChannel.vibrationPattern != null) {
                                vibrationPattern = notificationChannel.vibrationPattern
                            }
                            setShowBadge(notificationChannel.showBadge)
                            if(notificationChannel.notificationChannelGroups != null){
                                group = notificationChannel.notificationChannelGroups.id
                            }
                            if(notificationChannel.sound != null){
                                val soundId = notificationChannel.sound
                                val uri = Uri.parse("android.resource://${context.packageName}/raw/$soundId")
                                setSound(
                                        uri,
                                        AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(
                                                AudioAttributes.USAGE_NOTIFICATION_EVENT).build()
                                )
                            }
                        })
            }
        }
    }
}