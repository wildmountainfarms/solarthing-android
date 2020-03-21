package me.retrodaredevil.solarthing.android

import androidx.multidex.MultiDexApplication
import me.retrodaredevil.solarthing.android.service.SolarEventData

class SolarThingApplication : MultiDexApplication() {
    @get:Synchronized
    @set:Synchronized
    var solarEventData: SolarEventData? = null
}
