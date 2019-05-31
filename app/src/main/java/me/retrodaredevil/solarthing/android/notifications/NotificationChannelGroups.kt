package me.retrodaredevil.solarthing.android.notifications

import android.content.Context
import me.retrodaredevil.solarthing.android.R

enum class NotificationChannelGroups(
    val id: String,
    private val nameResId: Int,
    private val descriptionResId: Int
) {
    SOLAR("solar", R.string.solar_group, R.string.solar_group_description),
    OUTHOUSE("outhouse", R.string.outhouse_group, R.string.outhouse_group_description);

    fun getName(context: Context): String {
        return context.getString(nameResId)
    }
    fun getDescription(context: Context): String {
        return context.getString(descriptionResId)
    }
}