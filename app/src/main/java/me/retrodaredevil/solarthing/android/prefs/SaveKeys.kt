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
    object NetworkSwitching {
        const val isEnabled = "is_enabled"
        const val isBackup = "is_backup"
        const val ssid = "ssid"
    }

    @Deprecated("We don't care about retaining this setting")
    const val voltageTimerTimeHours = "generator_float_hours" // use this save key for backwards compatibility // we will probably stop using this in the future anyway

    const val initialRequestTimeSeconds = "initial_request_timeout"
    const val subsequentRequestTimeSeconds = "subsequent_request_timeout"
    const val databaseConnectionProfile = "database_connection_profile"
    const val networkSwitchingProfile = "network_switching_profile"

    const val maxFragmentTimeMinutes = "max_fragment_time"

    @Deprecated("We don't care about retaining this setting")
    const val voltageTimerBatteryVoltage = "virtual_float_mode_minimum_battery_voltage"

    const val voltageTimerNodes = "voltage_timer_nodes"
    const val temperatureNodes = "temperature_nodes"
    const val lowBatteryVoltage = "low_battery_voltage"
    const val criticalBatteryVoltage = "critical_battery_voltage"
    const val batteryVoltageType = "battery_voltage_type"

    const val startOnBoot = "start_on_boot"
    const val networkSwitchingEnabled = "network_switching_enabled"

    const val connectionType = "connection_type"
    const val temperatureUnit = "temperature_unit"
}