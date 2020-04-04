package me.retrodaredevil.solarthing.android

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.android.prefs.gson.JsonProfileManager
import me.retrodaredevil.solarthing.android.prefs.gson.PreferencesJsonSaver
import me.retrodaredevil.solarthing.android.prefs.saving.BasicProfileManager
import me.retrodaredevil.solarthing.android.prefs.saving.PreferencesMiscProfileHolder
import me.retrodaredevil.solarthing.android.prefs.saving.SharedPreferencesStringValueSaver
import java.util.*

private const val SHARED_PREFERENCES = "profiles_preferences"

fun createConnectionProfileManager(context: Context): ProfileManager<ConnectionProfile> {

    return BasicProfileManager.createJacksonProfileManager(
        SharedPreferencesStringValueSaver(context.getDeviceProtectedStorageSharedPreferences(SHARED_PREFERENCES, 0), "connection_properties")
    ) { ConnectionProfile() }
}
fun createSolarProfileManager(context: Context): ProfileManager<SolarProfile> {
    return BasicProfileManager.createJacksonProfileManager(
        SharedPreferencesStringValueSaver(context.getDeviceProtectedStorageSharedPreferences(SHARED_PREFERENCES, 0), "solar_properties")
    ) {
        SolarProfile()
    }
}
fun createMiscProfileProvider(context: Context): ProfileProvider<MiscProfile> {
    val profile =
        PreferencesMiscProfileHolder(
            context.getDeviceProtectedStorageSharedPreferences("misc_settings", 0),
            context
        )
    return object : ProfileProvider<MiscProfile> {
        override val activeProfile: ProfileHolder<MiscProfile>
            get() = profile
    }
}
