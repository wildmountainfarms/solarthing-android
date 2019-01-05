package me.retrodaredevil.solarthing.android

object SaveKeys{
    object CouchDb {
        const val port = "port"
        const val databaseName = "database_name"
        const val host = "host"
        const val username = "username"
        const val password = "password"
        const val protocol = "protocol"
        const val useAuth = "use_auth"
    }

    const val generatorFloatTimeHours = "generator_float_hours"

    const val initialRequestTimeSeconds = "initial_request_timeout"

    const val subsequentRequestTimeSeconds = "subsequent_request_timeout"
    const val virtualFloatModeMinimumBatteryVoltage = "virtual_float_mode_minimum_battery_voltage"

}