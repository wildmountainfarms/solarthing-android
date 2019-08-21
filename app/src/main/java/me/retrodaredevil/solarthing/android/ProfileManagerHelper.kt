package me.retrodaredevil.solarthing.android

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.android.prefs.json.JsonConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.json.JsonProfileManager
import me.retrodaredevil.solarthing.android.prefs.json.JsonSolarProfile
import me.retrodaredevil.solarthing.android.prefs.json.PreferencesJsonSaver
import java.util.*

private const val SHARED_PREFERENCES = "profiles_preferences"

private fun createJsonProfileManagerJsonCreator(newProfileJsonCreator: () -> JsonObject): () -> JsonObject{
    return { JsonObject().apply {
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
}

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
    return JsonProfileManager(
        PreferencesJsonSaver(
            context.getSharedPreferences(SHARED_PREFERENCES, 0),
            "connection_properties",
            createJsonProfileManagerJsonCreator(newProfileJsonCreator)
        ),
        newProfileJsonCreator,
        ::JsonConnectionProfile
    )
}
fun createSolarProfileManager(context: Context): ProfileManager<SolarProfile> {

    val newProfileJsonCreator = { JsonObject().apply {
        addProperty(SaveKeys.criticalBatteryVoltage, DefaultOptions.criticalBatteryVoltage)
        addProperty(SaveKeys.lowBatteryVoltage, DefaultOptions.lowBatteryVoltage)
        addProperty(SaveKeys.generatorFloatTimeHours, DefaultOptions.generatorFloatTimeHours)
        addProperty(SaveKeys.virtualFloatModeMinimumBatteryVoltage, DefaultOptions.virtualFloatModeMinimumBatteryVoltage)
    } }
    return JsonProfileManager(
        PreferencesJsonSaver(
            context.getSharedPreferences(SHARED_PREFERENCES, 0),
            "solar_properties",
            createJsonProfileManagerJsonCreator(newProfileJsonCreator)
        ),
        newProfileJsonCreator,
        ::JsonSolarProfile
    )
}
fun createMiscProfileProvider(context: Context): ProfileProvider<MiscProfile> {
    val profile = PreferencesMiscProfile(context.getSharedPreferences("misc_settings", 0))
    return object : ProfileProvider<MiscProfile> {
        override val activeProfile: MiscProfile
            get() = profile
    }
}
