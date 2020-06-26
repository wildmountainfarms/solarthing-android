package me.retrodaredevil.solarthing.android

import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import me.retrodaredevil.solarthing.android.util.Formatting


class BatteryVoltageComplicationProviderService : ComplicationProviderService() {

    override fun onComplicationUpdate(complicationId: Int, type: Int, manager: ComplicationManager) {
        check(type == ComplicationData.TYPE_SHORT_TEXT) { "(Currently) Unsupported type: $type "}

        val application = application as SolarThingWearApplication

        val dataMap = application.basicSolarDataMap
        val data = dataMap?.let { BasicSolarData.fromDataMap(it) }
        if (data == null || application.isDataOld) {
            println("null or old! data: $data")
            manager.updateComplicationData(
                    complicationId,
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("??.?V"))
                            .build()
            )
            return
        }
        val iconResource = when {
            data.acMode == 2 -> R.drawable.plug
            data.anyChargeControllerOn -> R.drawable.charging
            else -> R.drawable.moon
        }

        println("Battery Voltage is: ${data.batteryVoltage}")
        manager.updateComplicationData(
                complicationId,
                ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setIcon(Icon.createWithResource(this, iconResource))
                        .setShortText(ComplicationText.plainText(data.batteryVoltage + "V"))
                        .build()
        )
    }
}
