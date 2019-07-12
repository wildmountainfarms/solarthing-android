package me.retrodaredevil.solarthing.android

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.support.v4.content.LocalBroadcastManager
import android.widget.RemoteViews
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.android.service.SolarPacketCollectionBroadcast
import me.retrodaredevil.solarthing.packets.collection.PacketCollections
import me.retrodaredevil.solarthing.solar.SolarPackets

/**
 * Implementation of App Widget functionality.
 */
class BatteryVoltageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        throw IllegalStateException()
    }

    fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, latestInfo: SolarPacketInfo?) {
        // There may be multiple widgets active, so update all of them
        println("updating widgets. latestInfo: $latestInfo updating: ${appWidgetIds.size}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, latestInfo)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent!!.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE){
            val extras = intent.extras
            if (extras != null) {
                val appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                val json = intent.getStringExtra(SolarPacketCollectionBroadcast.JSON)

                if (json != null && appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    println("got json:")
                    println(json)
                    val jsonObject = GSON.fromJson(json, JsonObject::class.java)
                    val packetCollection = PacketCollections.createFromJson(jsonObject, SolarPackets::createFromJson)
                    val info = SolarPacketInfo(packetCollection)
                    onUpdate(context!!, AppWidgetManager.getInstance(context), appWidgetIds, info)
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }


    companion object {
        private val GSON = GsonBuilder().create()

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

