package me.retrodaredevil.solarthing.android.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarThingApplication
import me.retrodaredevil.solarthing.android.createConnectionProfileManager
import me.retrodaredevil.solarthing.android.createSolarProfileManager
import me.retrodaredevil.solarthing.android.data.CreationException
import me.retrodaredevil.solarthing.android.data.SolarPacketInfo
import me.retrodaredevil.solarthing.packets.collection.DefaultInstanceOptions
import me.retrodaredevil.solarthing.packets.collection.PacketGroups

/**
 * Implementation of App Widget functionality.
 */
class BatteryVoltageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) { // called every 30 minutes, or when onReceive calls it
        // There may be multiple widgets active, so update all of them
        val latestInfo = getLatestInfo(context)
        println("Updating battery voltage widgets. IDs: ${appWidgetIds.contentToString()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, latestInfo)
        }
    }

    private fun getLatestInfo(context: Context): SolarPacketInfo? {
        val applicationContext = context.applicationContext
        return if (applicationContext is SolarThingApplication) {
            val solarStatusData = applicationContext.solarStatusData
            if (solarStatusData != null) {
                val sorted = PacketGroups.sortPackets(solarStatusData.packetGroups, DefaultInstanceOptions.DEFAULT_DEFAULT_INSTANCE_OPTIONS, 10 * 60 * 1000, 10 * 60 * 1000)
                if (sorted.isEmpty()) {
                    System.err.println("Sorted is empty!")
                    null
                } else {
                    val sourceId = createConnectionProfileManager(context).activeProfile.profile.selectSourceId(sorted.keys)
                    val packets = sorted[sourceId]!!
                    if (packets.isEmpty()) {
                        throw AssertionError("One of the values of sortPackets() should never be empty!")
                    }
                    try {
                        val manager = createSolarProfileManager(context)
                        val batteryVoltageType = manager.activeProfile.profile.batteryVoltageType
                        SolarPacketInfo(packets.last(), batteryVoltageType) // the most recent info
                    } catch (ex: CreationException) {
                        ex.printStackTrace()
                        null
                    }
                }
            } else {
                System.err.println("So we got an event to update, but the data is null? Weird...")
                null
            }
        } else {
            System.err.println("BAD!! What kind of Android version are we running here? Now we can't update this widget!")
            null
        }
    }

    companion object {

        internal fun updateAppWidget(
                context: Context, appWidgetManager: AppWidgetManager,
                appWidgetId: Int,
                latestInfo: SolarPacketInfo?
        ) {

            val widgetText = if (latestInfo == null || System.currentTimeMillis() - latestInfo.dateMillis > 5 * 60 * 1000) "??.?" else latestInfo.batteryVoltageString

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.battery_voltage_widget)
            views.setTextViewText(R.id.battery_voltage_widget_text, widgetText)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

