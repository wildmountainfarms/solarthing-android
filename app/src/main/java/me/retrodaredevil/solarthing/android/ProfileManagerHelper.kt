package me.retrodaredevil.solarthing.android

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.android.prefs.gson.JsonConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.gson.JsonProfileManager
import me.retrodaredevil.solarthing.android.prefs.gson.JsonSolarProfile
import me.retrodaredevil.solarthing.android.prefs.gson.PreferencesJsonSaver
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

    val newProfileJsonCreator = ::JsonObject
    return JsonProfileManager(
        PreferencesJsonSaver(
            context.getDeviceProtectedStorageSharedPreferences(SHARED_PREFERENCES, 0),
            "connection_properties",
            createJsonProfileManagerJsonCreator(newProfileJsonCreator)
        ),
        newProfileJsonCreator,
        ::JsonConnectionProfile
    )
}
fun createSolarProfileManager(context: Context): ProfileManager<SolarProfile> {

    val newProfileJsonCreator = ::JsonObject
    return JsonProfileManager(
        PreferencesJsonSaver(
            context.getDeviceProtectedStorageSharedPreferences(SHARED_PREFERENCES, 0),
            "solar_properties",
            createJsonProfileManagerJsonCreator(newProfileJsonCreator)
        ),
        newProfileJsonCreator,
        ::JsonSolarProfile
    )
}
fun createMiscProfileProvider(context: Context): ProfileProvider<MiscProfile> {
    val profile = PreferencesMiscProfile(context.getDeviceProtectedStorageSharedPreferences("misc_settings", 0), context)
    return object : ProfileProvider<MiscProfile> {
        override val activeProfile: MiscProfile
            get() = profile
    }
}
