package me.retrodaredevil.solarthing.android.prefs

object SaveKeys{
    object CouchDb {
        const val port = "port"
        const val host = "host"
        const val username = "username"
        const val password = "password"
        const val protocol = "protocol"
        const val useAuth = "use_auth"
    }

    const val generatorFloatTimeHours = "generator_float_hours"

    const val initialRequestTimeSeconds = "initial_request_timeout"
    const val subsequentRequestTimeSeconds = "subsequent_request_timeout"
    const val databaseConnectionProfile = "database_connection_profile"

    const val maxFragmentTimeMinutes = "max_fragment_time"

    const val virtualFloatModeMinimumBatteryVoltage = "virtual_float_mode_minimum_battery_voltage"

    const val lowBatteryVoltage = "low_battery_voltage"
    const val criticalBatteryVoltage = "critical_battery_voltage"

    const val startOnBoot = "start_on_boot"
}