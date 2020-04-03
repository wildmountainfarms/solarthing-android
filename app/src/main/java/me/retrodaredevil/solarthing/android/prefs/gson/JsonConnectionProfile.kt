package me.retrodaredevil.solarthing.android.prefs.gson

import me.retrodaredevil.solarthing.android.prefs.*

@Deprecated("This is part of the old prefs code")
class JsonConnectionProfile(
    private val jsonSaver: JsonSaver
) : ConnectionProfile {
    override val databaseConnectionProfile: DatabaseConnectionProfile = JsonDatabaseConnectionProfile(NestedJsonSaver(
        jsonSaver, SaveKeys.databaseConnectionProfile
    ))
    override val networkSwitchingProfile: NetworkSwitchingProfile = JsonNetworkSwitchingProfile(NestedJsonSaver(
        jsonSaver, SaveKeys.networkSwitchingProfile
    ))


    override var initialRequestTimeSeconds: Int
        get() = jsonSaver.getAsInt(SaveKeys.initialRequestTimeSeconds, DefaultOptions.initialRequestTimeSeconds)!!
        set(value) {
            jsonSaver[SaveKeys.initialRequestTimeSeconds] = value
        }
    override var subsequentRequestTimeSeconds: Int
        get() = jsonSaver.getAsInt(SaveKeys.subsequentRequestTimeSeconds, DefaultOptions.subsequentRequestTimeSeconds)!!
        set(value) {
            jsonSaver[SaveKeys.subsequentRequestTimeSeconds] = value
        }
    override var requestWaitTimeSeconds: Int
        get() = TODO("not implemented")
        set(value) = TODO("not implemented")

}
@Deprecated("This is part of the old prefs code")
internal class JsonDatabaseConnectionProfile(
    private val jsonSaver: JsonSaver
) : CouchDbDatabaseConnectionProfile {
    override var protocol: String
        get() = jsonSaver.getAsString(SaveKeys.CouchDb.protocol, DefaultOptions.CouchDb.protocol)!!
        set(value) {
            jsonSaver[SaveKeys.CouchDb.protocol] = value
        }
    override var host: String
        get() = jsonSaver.getAsString(SaveKeys.CouchDb.host, DefaultOptions.CouchDb.host)!!
        set(value) {
            jsonSaver[SaveKeys.CouchDb.host] = value
        }
    override var port: Int
        get() = jsonSaver.getAsInt(SaveKeys.CouchDb.port, DefaultOptions.CouchDb.port)!!
        set(value) {
            jsonSaver[SaveKeys.CouchDb.port] = value
        }

    override var username: String
        get() = jsonSaver.getAsString(SaveKeys.CouchDb.username, DefaultOptions.CouchDb.username)!!
        set(value) {
            jsonSaver[SaveKeys.CouchDb.username] = value
        }
    override var password: String
        get() = jsonSaver.getAsString(SaveKeys.CouchDb.password) ?: DefaultOptions.CouchDb.password
        set(value) {
            jsonSaver[SaveKeys.CouchDb.password] = value
        }
    override var useAuth: Boolean
        get() = jsonSaver.getAsBoolean(SaveKeys.CouchDb.useAuth) ?: DefaultOptions.CouchDb.useAuth
        set(value) {
            jsonSaver[SaveKeys.CouchDb.useAuth] = value
        }
}
@Deprecated("This is part of the old prefs code")
internal class JsonNetworkSwitchingProfile(
    private val jsonSaver: JsonSaver
) : NetworkSwitchingProfile {
    override var isEnabled: Boolean
        get() = jsonSaver.getAsBoolean(SaveKeys.NetworkSwitching.isEnabled) ?: DefaultOptions.NetworkSwitching.isEnabled
        set(value) {
            jsonSaver[SaveKeys.NetworkSwitching.isEnabled] = value
        }
    override var isBackup: Boolean
        get() = jsonSaver.getAsBoolean(SaveKeys.NetworkSwitching.isBackup) ?: DefaultOptions.NetworkSwitching.isBackup
        set(value) {
            jsonSaver[SaveKeys.NetworkSwitching.isBackup] = value
        }
    override var ssid: String?
        get() = jsonSaver.getAsString(SaveKeys.NetworkSwitching.ssid) ?: DefaultOptions.NetworkSwitching.ssid
        set(value) {
            jsonSaver[SaveKeys.NetworkSwitching.ssid] = value
        }

}
