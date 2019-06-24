package me.retrodaredevil.solarthing.android

import android.content.Context
import org.lightcouch.CouchDbProperties
import java.lang.IllegalArgumentException

class Prefs(private val context: Context) {

    private val connectionPreferences by lazy { context.getSharedPreferences("connection_properties", 0) }
    private val settings by lazy { context.getSharedPreferences("settings", 0) }


    inner class CouchDb internal constructor() {

        var protocol: String
            get() = connectionPreferences.getString(SaveKeys.CouchDb.protocol, null) ?: DefaultOptions.CouchDb.protocol
            set(value) = connectionPreferences.edit().putString(SaveKeys.CouchDb.protocol, value).apply()

        var host: String
            get() = connectionPreferences.getString(SaveKeys.CouchDb.host, null) ?: DefaultOptions.CouchDb.host
            set(value) = connectionPreferences.edit().putString(SaveKeys.CouchDb.host, value).apply()

        var port: Int
            get() = connectionPreferences.getInt(SaveKeys.CouchDb.port, DefaultOptions.CouchDb.port)
            set(value) = connectionPreferences.edit().putInt(SaveKeys.CouchDb.port, value).apply()

        var username: String
            get() = connectionPreferences.getString(SaveKeys.CouchDb.username, null) ?: DefaultOptions.CouchDb.username
            set(value) = connectionPreferences.edit().putString(SaveKeys.CouchDb.username, value).apply()

        var password: String
            get() = connectionPreferences.getString(SaveKeys.CouchDb.password, null) ?: DefaultOptions.CouchDb.password
            set(value) = connectionPreferences.edit().putString(SaveKeys.CouchDb.password, value).apply()

        var useAuth: Boolean
            get() = connectionPreferences.getBoolean(SaveKeys.CouchDb.useAuth, DefaultOptions.CouchDb.useAuth)
            set(value) = connectionPreferences.edit().putBoolean(SaveKeys.CouchDb.useAuth, value).apply()
    }
    val couchDb = CouchDb()

    fun createCouchDbProperties(): List<CouchDbProperties> {
        val username: String?
        val password: String?
        if(couchDb.useAuth){
            username = couchDb.username
            password = couchDb.password
        } else {
            username = null
            password = null
        }
        val protocol = couchDb.protocol
        val port = couchDb.port
        val hostsString = couchDb.host
        val hosts = hostsString.split(",")

        return hosts.map {
            CouchDbProperties(
                null,
                false,
                protocol,
                it,
                port,
                username,
                password
            )
        }
    }

    var generatorFloatTimeHours: Float
        get() = settings.getFloat(SaveKeys.generatorFloatTimeHours, DefaultOptions.generatorFloatTimeHours)
        set(value) = settings.edit().putFloat(SaveKeys.generatorFloatTimeHours, value).apply()

    var initialRequestTimeSeconds: Int
        get() = settings.getInt(SaveKeys.initialRequestTimeSeconds, DefaultOptions.initialRequestTimeSeconds)
        set(value) = settings.edit().putInt(SaveKeys.initialRequestTimeSeconds, value).apply()

    var subsequentRequestTimeSeconds: Int
        get() = settings.getInt(SaveKeys.subsequentRequestTimeSeconds, DefaultOptions.subsequentRequestTimeSeconds)
        set(value) = settings.edit().putInt(SaveKeys.subsequentRequestTimeSeconds, value).apply()

    var virtualFloatModeMinimumBatteryVoltage: Float?
        get() {
            val r = settings.getFloat(SaveKeys.virtualFloatModeMinimumBatteryVoltage, DefaultOptions.virtualFloatModeMinimumBatteryVoltage ?: -1F)
            if(r < 0){
                return null
            }
            return r
        }
        set(value) = when {
            value == null -> settings.edit().remove(SaveKeys.virtualFloatModeMinimumBatteryVoltage).apply()
            value < 0 -> throw IllegalArgumentException("Please use null instead of a negative value!")
            else -> settings.edit().putFloat(SaveKeys.virtualFloatModeMinimumBatteryVoltage, value).apply()
        }

    var lowBatteryVoltage: Float?
        get() {
            val r = settings.getFloat(SaveKeys.lowBatteryVoltage, DefaultOptions.lowBatteryVoltage ?: -1F)
            if(r < 0){
                return null
            }
            return r
        }
        set(value) = when {
            value == null -> settings.edit().remove(SaveKeys.lowBatteryVoltage).apply()
            value < 0 -> throw IllegalArgumentException("Please use null instead of a negative value!")
            else -> settings.edit().putFloat(SaveKeys.lowBatteryVoltage, value).apply()
        }
    var criticalBatteryVoltage: Float?
        get() {
            val r = settings.getFloat(SaveKeys.criticalBatteryVoltage, DefaultOptions.criticalBatteryVoltage ?: -1F)
            if(r < 0){
                return null
            }
            return r
        }
        set(value) = when {
            value == null -> settings.edit().remove(SaveKeys.criticalBatteryVoltage).apply()
            value < 0 -> throw IllegalArgumentException("Please use null instead of a negative value!")
            else -> settings.edit().putFloat(SaveKeys.criticalBatteryVoltage, value).apply()
        }


    var startOnBoot: Boolean
        get() = settings.getBoolean(SaveKeys.startOnBoot, DefaultOptions.startOnBoot)
        set(value) = settings.edit().putBoolean(SaveKeys.startOnBoot, value).apply()

}