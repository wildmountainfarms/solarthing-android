package me.retrodaredevil.solarthing.android.notifications

import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.Color
import android.os.Build
import me.retrodaredevil.solarthing.android.R

@TargetApi(Build.VERSION_CODES.N)
enum class NotificationChannels(
    val id: String,
    private val nameResId: Int,
    private val descriptionResId: Int,
    val importance: Int,
    val enableLights: Boolean = false,
    val lightColor: Int? = null,
    val enableVibration: Boolean = false,
    val vibrationPattern: LongArray? = null,
    val showBadge: Boolean = false,
    val notificationChannelGroups: NotificationChannelGroups? = null
) {
    PERSISTENT("persistent",
        R.string.persistent,
        R.string.persistent_description, NotificationManager.IMPORTANCE_MIN),
    SOLAR_STATUS("solar_status",
        R.string.solar_status,
        R.string.solar_status_description, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.SOLAR),

    OUTHOUSE_STATUS_WHILE_VACANT("outhouse_status_vacant",
        R.string.outhouse_status_vacant,
        R.string.outhouse_status_vacant_description, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE),
    OUTHOUSE_STATUS_WHILE_OCCUPIED("outhouse_status_occupied",
        R.string.outhouse_status_occupied,
        R.string.outhouse_status_occupied_description, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE),

    GENERATOR_FLOAT_NOTIFICATION("generator_float_notification",
        R.string.generator_float_notification,
        R.string.generator_float_notification_status, NotificationManager.IMPORTANCE_HIGH,
        enableLights = true, lightColor = Color.CYAN, showBadge = true, notificationChannelGroups = NotificationChannelGroups.SOLAR),
    GENERATOR_DONE_NOTIFICATION("generator_done_notification",
        R.string.generator_done_notification,
        R.string.generator_done_notification, NotificationManager.IMPORTANCE_HIGH,
        enableLights = true, lightColor = Color.CYAN, showBadge = true, notificationChannelGroups = NotificationChannelGroups.SOLAR),

    VACANT_NOTIFICATION("vacant_notification",
        R.string.vacant_notification,
        R.string.vacant_notification_status, NotificationManager.IMPORTANCE_HIGH, enableLights = true, lightColor = Color.GREEN, showBadge = true, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE),
    SILENT_VACANT_NOTIFICATION("silent_vacant_notification",
        R.string.silent_vacant_notification,
        R.string.silent_vacant_notification_status, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE)
    ;


    fun getName(context: Context): String = context.getString(nameResId)
    fun getDescription(context: Context): String = context.getString(descriptionResId)

    fun isCurrentlyEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if(notificationChannelGroups != null){
                if(manager.getNotificationChannelGroup(notificationChannelGroups.id).isBlocked){
                    return false
                }
            }
            val importance = manager.getNotificationChannel(this.id).importance
            return importance != NotificationManager.IMPORTANCE_NONE
        }
        return manager.areNotificationsEnabled()
    }
}