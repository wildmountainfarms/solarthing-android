package me.retrodaredevil.solarthing.android

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.fasterxml.jackson.databind.node.ObjectNode
import me.retrodaredevil.solarthing.android.prefs.BatteryVoltageType
import me.retrodaredevil.solarthing.android.service.SolarPacketCollectionBroadcast
import me.retrodaredevil.solarthing.packets.collection.parsing.ObjectMapperPacketConverter
import me.retrodaredevil.solarthing.packets.collection.parsing.PacketParseException
import me.retrodaredevil.solarthing.packets.collection.parsing.PacketParserMultiplexer
import me.retrodaredevil.solarthing.packets.collection.parsing.SimplePacketGroupParser
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.util.JacksonUtil

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

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent!!.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE){
            val extras = intent.extras
            if (extras != null) {
                val appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                val json = intent.getStringExtra(SolarPacketCollectionBroadcast.JSON)

                if (json != null && appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    println("got json:")
                    println(json)
                    try {
                        val packetGroup = PARSER.parse(MAPPER.readTree(json) as ObjectNode)
                        val info = SolarPacketInfo(packetGroup, BatteryVoltageType.FIRST_PACKET) // TODO don't hard code BatteryVoltageType
                        onUpdate(context!!, AppWidgetManager.getInstance(context), appWidgetIds, info)
                    } catch(ex: PacketParseException){
                        ex.printStackTrace()
                    }
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }


    companion object {
        private val MAPPER = createDefaultObjectMapper()
        private val PARSER = SimplePacketGroupParser(PacketParserMultiplexer(listOf(
            ObjectMapperPacketConverter(MAPPER, SolarStatusPacket::class.java)
        ), PacketParserMultiplexer.LenientType.FULLY_LENIENT))

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

