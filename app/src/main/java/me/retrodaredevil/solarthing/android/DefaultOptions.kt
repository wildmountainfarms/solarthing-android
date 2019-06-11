package me.retrodaredevil.solarthing.android

object DefaultOptions {
    object CouchDb {
        const val port = 5984
        const val host = "192.168.1.110"
        const val username = "admin"
        const val password = "relax"
        const val protocol = "http"
        const val useAuth = false
    }

    const val generatorFloatTimeHours = 1.5F

    const val generatorNotifyIntervalMillis = 5 * 60 * 1000L

    const val initialRequestTimeSeconds = 60

    const val subsequentRequestTimeSeconds = 30
    val virtualFloatModeMinimumBatteryVoltage: Float? = null

    const val startOnBoot = true
}