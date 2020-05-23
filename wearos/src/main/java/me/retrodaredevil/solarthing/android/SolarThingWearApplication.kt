package me.retrodaredevil.solarthing.android

import android.app.Application

class SolarThingWearApplication : Application() {

    @Volatile
    var batteryVoltage: Float? = null
}
