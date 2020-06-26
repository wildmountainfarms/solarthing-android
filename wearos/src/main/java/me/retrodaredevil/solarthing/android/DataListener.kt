package me.retrodaredevil.solarthing.android

import android.support.wearable.complications.ProviderUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class DataListener : WearableListenerService() {

    override fun onDataChanged(dataEvent: DataEventBuffer) {
        for (event in dataEvent) {
            val item = event.dataItem!!
            when (item.uri.path) {
                BasicSolarData.PATH -> {
                    println("Got basic solar data")
                    val application = application as SolarThingWearApplication
                    application.basicSolarDataMap = DataMapItem.fromDataItem(item).dataMap
                    requestUpdate()
                }
                HeartbeatData.PATH -> {
                    val application = application as SolarThingWearApplication
                    application.lastHeartbeat = DataMapItem.fromDataItem(item).dataMap.getLong(HeartbeatData.DATE_MILLIS)
                    requestUpdate()
                }
            }
        }
    }
    private fun requestUpdate() {
        for (componentName in WearConstants.PROVIDER_COMPONENT_NAMES) {
            val requester = ProviderUpdateRequester(
                    this,
                    componentName
            )
            requester.requestUpdateAll()
        }
    }
}
