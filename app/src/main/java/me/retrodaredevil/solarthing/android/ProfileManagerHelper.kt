package me.retrodaredevil.solarthing.android

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.android.prefs.ConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.DefaultOptions
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import me.retrodaredevil.solarthing.android.prefs.SaveKeys
import me.retrodaredevil.solarthing.android.prefs.json.JsonConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.json.JsonProfileManager
import me.retrodaredevil.solarthing.android.prefs.json.PreferencesJsonSaver
import java.util.*

fun createConnectionProfileManager(context: Context): ProfileManager<ConnectionProfile> {

    val newProfileJsonCreator = { JsonObject().apply {
        addProperty(SaveKeys.initialRequestTimeSeconds, DefaultOptions.initialRequestTimeSeconds)
        addProperty(SaveKeys.subsequentRequestTimeSeconds, DefaultOptions.subsequentRequestTimeSeconds)
        val databaseConnection = JsonObject()
        databaseConnection.apply {
            addProperty(SaveKeys.CouchDb.protocol, DefaultOptions.CouchDb.protocol)
            addProperty(SaveKeys.CouchDb.port, DefaultOptions.CouchDb.port)
            addProperty(SaveKeys.CouchDb.host, DefaultOptions.CouchDb.host)
            addProperty(SaveKeys.CouchDb.username, DefaultOptions.CouchDb.username)
            addProperty(SaveKeys.CouchDb.password, DefaultOptions.CouchDb.password)
            addProperty(SaveKeys.CouchDb.useAuth, DefaultOptions.CouchDb.useAuth)
        }
        add(SaveKeys.databaseConnectionProfile, databaseConnection)
    } }
    val jsonProfileManagerJsonCreator = { JsonObject().apply {
        val uuid = UUID.randomUUID()
        addProperty("active", uuid.toString())
        val array = JsonArray()
        val defaultJsonObject = JsonObject().apply {
            val defaultJsonProfile = newProfileJsonCreator()
            addProperty("uuid", uuid.toString())
            addProperty("name", "Default Profile")
            add("profile", defaultJsonProfile)
        }
        array.add(defaultJsonObject)
        add("profiles", array)
    } }
    return JsonProfileManager(
        PreferencesJsonSaver(context.getSharedPreferences("profiles_preferences1", 0), "connection_properties", jsonProfileManagerJsonCreator),
        newProfileJsonCreator,
        ::JsonConnectionProfile
    )
}
