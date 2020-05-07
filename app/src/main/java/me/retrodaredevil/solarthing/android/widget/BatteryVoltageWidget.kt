package me.retrodaredevil.solarthing.android.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarThingApplication
import me.retrodaredevil.solarthing.android.data.SolarPacketInfo
import me.retrodaredevil.solarthing.android.prefs.BatteryVoltageType
import me.retrodaredevil.solarthing.packets.collection.DefaultInstanceOptions
import me.retrodaredevil.solarthing.packets.collection.PacketGroups

/**
 * Implementation of App Widget functionality.
 */
class BatteryVoltageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
    }

    private fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, latestInfo: SolarPacketInfo?) {
        // There may be multiple widgets active, so update all of them
        println("updating widgets. latestInfo: $latestInfo updating: ${appWidgetIds.size}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, latestInfo)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE){
            val extras = intent.extras
            if (extras != null) {
                val appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                appWidgetIds ?: return
                val applicationContext = context.applicationContext
                if (applicationContext is SolarThingApplication) {
                    val solarStatusData = applicationContext.solarStatusData
                    if (solarStatusData != null) {
                        val sorted = PacketGroups.sortPackets(solarStatusData.packetGroups, 10 * 60 * 1000)
                        onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds, SolarPacketInfo(sorted.values.first().last(), BatteryVoltageType.FIRST_PACKET))
                    } else {
                        System.err.println("So we got an event to update, but the data is null? Weird...")
                        onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds, null)
                    }
                } else {
                    System.err.println("BAD!! What kind of Android version are we running here? Now we can't update this widget!")
                    onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds, null)
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }


    companion object {

        internal fun updateAppWidget(
            context: Context, appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            latestInfo: SolarPacketInfo?
        ) {

            val widgetText = latestInfo?.batteryVoltageString ?: "??.?"
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.battery_voltage_widget)
            views.setTextViewText(R.id.appwidget_text, widgetText)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

