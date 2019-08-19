package me.retrodaredevil.solarthing.android.prefs.json

import me.retrodaredevil.solarthing.android.prefs.ConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.CouchDbDatabaseConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.DatabaseConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.SaveKeys

class JsonConnectionProfile(
    private val jsonSaver: JsonSaver
) : ConnectionProfile {
    override val databaseConnectionProfile: DatabaseConnectionProfile = JsonDatabaseConnectionProfile(NestedJsonSaver(
        jsonSaver,"databaseConnectionProfile"
    ))


    override var initialRequestTimeSeconds: Int
        get() = jsonSaver.jsonObject.get(SaveKeys.initialRequestTimeSeconds).asInt
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.initialRequestTimeSeconds, value)
            jsonSaver.save()
        }
    override var subsequentRequestTimeSeconds: Int
        get() = jsonSaver.jsonObject.get(SaveKeys.subsequentRequestTimeSeconds).asInt
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.subsequentRequestTimeSeconds, value)
            jsonSaver.save()
        }
    override var requestWaitTimeSeconds: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

}
internal class JsonDatabaseConnectionProfile(
    private val jsonSaver: JsonSaver
) : CouchDbDatabaseConnectionProfile {
    override var protocol: String
        get() = jsonSaver.jsonObject.get(SaveKeys.CouchDb.protocol).asString
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.CouchDb.protocol, value)
            jsonSaver.save()
        }
    override var host: String
        get() = jsonSaver.jsonObject.get(SaveKeys.CouchDb.host).asString
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.CouchDb.host, value)
            jsonSaver.save()
        }
    override var port: Int
        get() = jsonSaver.jsonObject.get(SaveKeys.CouchDb.port).asInt
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.CouchDb.port, value)
            jsonSaver.save()
        }

    override var username: String
        get() = jsonSaver.jsonObject.get(SaveKeys.CouchDb.username).asString
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.CouchDb.username, value)
            jsonSaver.save()
        }
    override var password: String
        get() = jsonSaver.jsonObject.get(SaveKeys.CouchDb.password).asString
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.CouchDb.password, value)
            jsonSaver.save()
        }
    override var useAuth: Boolean
        get() = jsonSaver.jsonObject.get(SaveKeys.CouchDb.useAuth).asBoolean
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.CouchDb.useAuth, value)
            jsonSaver.save()
        }
}