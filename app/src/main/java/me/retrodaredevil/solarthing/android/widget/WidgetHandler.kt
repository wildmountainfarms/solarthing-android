package me.retrodaredevil.solarthing.android.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import me.retrodaredevil.solarthing.android.service.SolarPacketCollectionBroadcast

class WidgetHandler : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != SolarPacketCollectionBroadcast.ACTION) {
            throw UnsupportedOperationException()
        }
        packetReceive(context, intent)
    }
    private fun packetReceive(context: Context, @Suppress("UNUSED_PARAMETER") intent: Intent) {
        println("Received solar packet collection broadcast and now updating all Battery Voltage Widgets!")
        context.sendBroadcast(Intent(context, BatteryVoltageWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, BatteryVoltageWidget::class.java)
            ))
        })
    }

}