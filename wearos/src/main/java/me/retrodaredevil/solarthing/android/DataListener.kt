package me.retrodaredevil.solarthing.android

import android.content.ComponentName
import android.support.wearable.complications.ProviderUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class DataListener : WearableListenerService() {

    override fun onDataChanged(dataEvent: DataEventBuffer) {
        for (event in dataEvent) {
            val item = event.dataItem!!
            if (item.uri.path!!.compareTo("/battery") == 0) {
                val batteryVoltage = DataMapItem.fromDataItem(item).dataMap.getFloat("batteryVoltage")
                println("Battery voltage is now: $batteryVoltage")
                val application = application as SolarThingWearApplication
                application.batteryVoltage = batteryVoltage
                requestUpdate()
                return
            }
        }
    }
    private fun requestUpdate() {
        val requester = ProviderUpdateRequester(
            this,
            ComponentName(BatteryVoltageComplicationProviderService::class.java.`package`!!.name, BatteryVoltageComplicationProviderService::class.java.name)
        )
        requester.requestUpdateAll()
    }
}
