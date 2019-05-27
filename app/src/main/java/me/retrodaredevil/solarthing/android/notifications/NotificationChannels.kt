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
    val showBadge: Boolean = false
) {
    PERSISTENT("persistent",
        R.string.persistent,
        R.string.persistent_description, NotificationManager.IMPORTANCE_MIN),
    SOLAR_STATUS("solar_status",
        R.string.solar_status,
        R.string.solar_status_description, NotificationManager.IMPORTANCE_LOW),

    OUTHOUSE_STATUS_WHILE_VACANT("outhouse_status_vacant",
        R.string.outhouse_status_vacant,
        R.string.outhouse_status_vacant_description, NotificationManager.IMPORTANCE_LOW),
    OUTHOUSE_STATUS_WHILE_OCCUPIED("outhouse_status_occupied",
        R.string.outhouse_status_occupied,
        R.string.outhouse_status_occupied_description, NotificationManager.IMPORTANCE_LOW),

    GENERATOR_NOTIFICATION("generator_notification",
        R.string.generator_notification,
        R.string.generator_notification_status, NotificationManager.IMPORTANCE_HIGH, enableLights = true, lightColor = Color.CYAN, showBadge = true),
    VACANT_NOTIFICATION("vacant_notification",
        R.string.vacant_notification,
        R.string.vacant_notification_status, NotificationManager.IMPORTANCE_HIGH, enableLights = true, lightColor = Color.GREEN, showBadge = true),
    SILENT_VACANT_NOTIFICATION("silent_vacant_notification",
        R.string.silent_vacant_notification,
        R.string.silent_vacant_notification_status, NotificationManager.IMPORTANCE_LOW)
    ;


    fun getName(context: Context): String = context.getString(nameResId)
    fun getDescription(context: Context): String = context.getString(descriptionResId)

    fun isCurrentlyEnabled(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val importance = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).getNotificationChannel(this.id).importance
        importance != NotificationManager.IMPORTANCE_NONE
    } else {
        true
    }
}