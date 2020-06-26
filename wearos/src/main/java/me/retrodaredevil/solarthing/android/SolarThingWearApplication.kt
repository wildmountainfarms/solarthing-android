package me.retrodaredevil.solarthing.android

import android.app.Application
import com.google.android.gms.wearable.DataMap

class SolarThingWearApplication : Application() {

    @Volatile
    var basicSolarDataMap: DataMap? = null
    @Volatile
    var lastHeartbeat: Long? = null

    val isDataOld: Boolean
        get() {
            val lastHeartbeat = this.lastHeartbeat ?: return true
            return lastHeartbeat + 6 * 60 * 1000 < System.currentTimeMillis()
        }
}
