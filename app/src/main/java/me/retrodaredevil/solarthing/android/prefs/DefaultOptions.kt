package me.retrodaredevil.solarthing.android.prefs

import me.retrodaredevil.solarthing.android.data.TemperatureUnit

object DefaultOptions {
    object CouchDb {
        const val port = 5984
        const val host = "192.168.1.110"
        const val username = "admin"
        const val password = "relax"
        const val protocol = "http"
        const val useAuth = false
    }
    object NetworkSwitching {
        const val isEnabled = false
        const val isBackup = false
        val ssid: String? = null
    }

    const val importantAlertIntervalMillis = 5 * 60 * 1000L

    const val initialRequestTimeSeconds = 60

    const val maxFragmentTimeMinutes = 2.0F

    const val subsequentRequestTimeSeconds = 30

    val preferredSourceId: String? = null

    val lowBatteryVoltage: Float? = null
    val criticalBatteryVoltage: Float? = null
    val batteryVoltageType = BatteryVoltageType.FIRST_OUTBACK

    const val startOnBoot = true
    const val networkSwitchingEnabled = false
    val temperatureUnit = TemperatureUnit.CELSIUS
}