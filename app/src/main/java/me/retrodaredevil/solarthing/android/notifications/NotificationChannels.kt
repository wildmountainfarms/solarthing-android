package me.retrodaredevil.solarthing.android.notifications

import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.Context
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
    val vibrationPattern: LongArray? = null
) {
    PERSISTENT_STATUS("persistent_status",
        R.string.persistent_status,
        R.string.persistent_status_description, NotificationManager.IMPORTANCE_DEFAULT),
    GENERATOR_NOTIFICATION("generator_notification",
        R.string.generator_notification,
        R.string.generator_notification_status, NotificationManager.IMPORTANCE_HIGH, enableLights = true, lightColor = Color.CYAN)
    ;


    fun getName(context: Context): String = context.getString(nameResId)
    fun getDescription(context: Context): String = context.getString(descriptionResId)

}