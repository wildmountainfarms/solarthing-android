package me.retrodaredevil.solarthing.android

import androidx.multidex.MultiDexApplication
import me.retrodaredevil.solarthing.android.service.PacketGroupData

class SolarThingApplication : MultiDexApplication() {
    @get:Synchronized
    @set:Synchronized
    var solarEventData: PacketGroupData? = null
    @get:Synchronized
    @set:Synchronized
    var solarStatusData: PacketGroupData? = null
}
