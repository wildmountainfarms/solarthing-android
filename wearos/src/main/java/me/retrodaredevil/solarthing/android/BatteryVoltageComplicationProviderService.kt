package me.retrodaredevil.solarthing.android

import android.app.PendingIntent
import android.content.ComponentName
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText


class BatteryVoltageComplicationProviderService : ComplicationProviderService() {

    override fun onComplicationUpdate(complicationId: Int, type: Int, manager: ComplicationManager) {
        check(type == ComplicationData.TYPE_SHORT_TEXT) { "(Current) Unsupported type: $type "}

        val application = application as SolarThingWearApplication

        val batteryVoltage = application.batteryVoltage

        val thisProvider = ComponentName(this, javaClass)
        val complicationPendingIntent: PendingIntent = getToggleIntent(this, thisProvider, complicationId)

        println("Battery Voltage is: $batteryVoltage")
        val data = ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
            .setShortText(ComplicationText.plainText(batteryVoltage?.let { "${it}V" } ?: "??.? V"))
            .setTapAction(complicationPendingIntent)
            .build()
        manager.updateComplicationData(complicationId, data)
    }
}
