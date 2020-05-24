package me.retrodaredevil.solarthing.android

import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText

class BatteryTemperatureComplicationProviderService : ComplicationProviderService() {
    override fun onComplicationUpdate(complicationId: Int, type: Int, manager: ComplicationManager) {
        check(type == ComplicationData.TYPE_SHORT_TEXT) { "(Currently) Unsupported type: $type "}

        val application = application as SolarThingWearApplication

        val dataMap = application.basicSolarDataMap
        val data = dataMap?.let { BasicSolarData.fromDataMap(it) }
        val batteryTemperatureString = data?.batteryTemperatureString
        if (data == null || data.isOld() || batteryTemperatureString == null) {
            println("null or old! data: $data batteryTemperatureString: $batteryTemperatureString")
            manager.updateComplicationData(
                    complicationId,
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setIcon(Icon.createWithResource(this, R.drawable.battery))
                            .setShortText(ComplicationText.plainText("??"))
                            .build()
            )
            return
        }
        println("Battery temperature is: $batteryTemperatureString")
        manager.updateComplicationData(
                complicationId,
                ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setIcon(Icon.createWithResource(this, R.drawable.battery))
                        .setShortText(ComplicationText.plainText(batteryTemperatureString))
                        .build()
        )
    }

}
