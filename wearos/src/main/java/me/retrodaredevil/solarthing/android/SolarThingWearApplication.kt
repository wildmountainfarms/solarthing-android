package me.retrodaredevil.solarthing.android

import android.app.Application
import com.google.android.gms.wearable.DataMap

class SolarThingWearApplication : Application() {

    @Volatile
    var basicSolarDataMap: DataMap? = null
}
