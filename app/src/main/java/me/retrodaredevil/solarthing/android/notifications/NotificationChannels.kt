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
    val notificationChannelGroups: NotificationChannelGroups? = null,
    val sound: Int? = null
) {
    PERSISTENT("persistent",
        R.string.persistent,
        R.string.persistent_description, NotificationManager.IMPORTANCE_MIN),
    SOLAR_STATUS("solar_status",
        R.string.solar_status,
        R.string.solar_status_description, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.SOLAR),
    GENERATOR_PERSISTENT("generator_persistent_v2",
        R.string.generator_persistent,
        R.string.generator_persistent_description, NotificationManager.IMPORTANCE_DEFAULT, notificationChannelGroups = NotificationChannelGroups.SOLAR, sound = R.raw.generator),
    COMMAND_FEEDBACK("command_feedback",
        R.string.command_feedback,
        R.string.command_feedback_description, NotificationManager.IMPORTANCE_DEFAULT, notificationChannelGroups = NotificationChannelGroups.SOLAR),
    MORE_SOLAR_INFO("channel_more_solar_info", // TODO maybe change this to just a silent category in the "Other" notification channel group
        R.string.more_solar_info,
        R.string.more_solar_info_description, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.SOLAR),

    OUTHOUSE_STATUS_WHILE_VACANT("outhouse_status_vacant",
        R.string.outhouse_status_vacant,
        R.string.outhouse_status_vacant_description, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE),
    OUTHOUSE_STATUS_WHILE_OCCUPIED("outhouse_status_occupied",
        R.string.outhouse_status_occupied,
        R.string.outhouse_status_occupied_description, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE),

    GENERATOR_DONE_NOTIFICATION("generator_done_notification_v2",
        R.string.generator_done_notification,
        R.string.generator_done_notification_description, NotificationManager.IMPORTANCE_HIGH,
        enableLights = true, lightColor = Color.CYAN, showBadge = true, notificationChannelGroups = NotificationChannelGroups.SOLAR),
    END_OF_DAY("mx_end_of_day",
        R.string.mx_end_of_day,
        R.string.mx_end_of_day_description, NotificationManager.IMPORTANCE_LOW,
        showBadge = true, notificationChannelGroups = NotificationChannelGroups.SOLAR),
    CONNECTION_STATUS("connection_status",
        R.string.connection_status,
        R.string.connection_status_description, NotificationManager.IMPORTANCE_DEFAULT,
        showBadge = true, notificationChannelGroups = NotificationChannelGroups.SOLAR),

    BATTERY_NOTIFICATION("battery_notification_v2",
        R.string.battery_notification,
        R.string.battery_notification_description, NotificationManager.IMPORTANCE_HIGH,
        enableLights = true, lightColor = Color.RED, showBadge = true, notificationChannelGroups = NotificationChannelGroups.SOLAR, sound = R.raw.battery),

    VACANT_NOTIFICATION("vacant_notification_v2",
        R.string.vacant_notification,
        R.string.vacant_notification_status, NotificationManager.IMPORTANCE_HIGH, enableLights = true, lightColor = Color.GREEN, showBadge = true, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE, sound=R.raw.toilet),
    SILENT_VACANT_NOTIFICATION("silent_vacant_notification",
        R.string.silent_vacant_notification,
        R.string.silent_vacant_notification_status, NotificationManager.IMPORTANCE_LOW, notificationChannelGroups = NotificationChannelGroups.OUTHOUSE)
    ;
    companion object {
        val OLD_CHANNELS = listOf("generator_done_notification", "generator_float_notification", "generator_persistent", "battery_notification", "vacant_notification")
    }


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