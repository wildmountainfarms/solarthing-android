package me.retrodaredevil.solarthing.android

import android.content.Context
import org.lightcouch.CouchDbProperties
import java.lang.IllegalArgumentException

class Prefs(private val context: Context) {

    private val connectionPreferences by lazy { context.getSharedPreferences("connection_properties", 0) }
    private val settings by lazy { context.getSharedPreferences("settings", 0) }

    var couchDbProperties: CouchDbProperties
        get() {
            val prefs = connectionPreferences
            return CouchDbProperties(
                prefs.getString(SaveKeys.CouchDb.databaseName, DefaultOptions.CouchDb.databaseName),
                false,
                prefs.getString(SaveKeys.CouchDb.protocol, DefaultOptions.CouchDb.protocol),
                prefs.getString(SaveKeys.CouchDb.host, DefaultOptions.CouchDb.host),
                prefs.getInt(SaveKeys.CouchDb.port, DefaultOptions.CouchDb.port),
                prefs.getString(SaveKeys.CouchDb.username, DefaultOptions.CouchDb.username),
                prefs.getString(SaveKeys.CouchDb.password, DefaultOptions.CouchDb.password)
            )
        }
        set(value) {
            val prefs = connectionPreferences
            prefs.edit()
                .putString(SaveKeys.CouchDb.databaseName, value.dbName)
                .putString(SaveKeys.CouchDb.protocol, value.protocol)
                .putString(SaveKeys.CouchDb.host, value.host)
                .putInt(SaveKeys.CouchDb.port, value.port)
                .putString(SaveKeys.CouchDb.username, value.username)
                .putString(SaveKeys.CouchDb.password, value.password)
                .apply()
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
            val r = settings.getFloat(SaveKeys.virtualFloatModeMinimumBatteryVoltage, -1F)
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

}