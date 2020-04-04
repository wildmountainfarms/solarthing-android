package me.retrodaredevil.solarthing.android

import android.content.Context
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.android.prefs.saving.*
import java.util.*

private const val PROFILE_SHARED_PREFERENCES = "profiles_preferences"
private const val SINGLE_SHARED_PREFERENCES = "single_preferences"

fun createConnectionProfileManager(context: Context): ProfileManager<ConnectionProfile> {

    return BasicProfileManager.createJacksonProfileManager(
        SharedPreferencesStringValueSaver(context.getDeviceProtectedStorageSharedPreferences(PROFILE_SHARED_PREFERENCES, 0), "connection_properties")
    ) { ConnectionProfile() }
}
fun createSolarProfileManager(context: Context): ProfileManager<SolarProfile> {
    return BasicProfileManager.createJacksonProfileManager(
        SharedPreferencesStringValueSaver(context.getDeviceProtectedStorageSharedPreferences(PROFILE_SHARED_PREFERENCES, 0), "solar_properties")
    ) {
        SolarProfile()
    }
}
fun createMiscProfileProvider(context: Context): ProfileProvider<MiscProfile> {
    val legacyProfile = PreferencesMiscProfileHolder(
        context.getDeviceProtectedStorageSharedPreferences("misc_settings", 0),
        context
    )

    val mapper = createDefaultObjectMapper()
    val stringValueSaver = SharedPreferencesStringValueSaver(context.getDeviceProtectedStorageSharedPreferences(SINGLE_SHARED_PREFERENCES, 0), "misc_settings")
    val profileHolder = JacksonProfileHolder(stringValueSaver, mapper.constructType(MiscProfile::class.java), mapper) {
//        MiscProfile(DefaultOptions.maxFragmentTimeMinutes, DefaultOptions.startOnBoot, DefaultOptions.networkSwitchingEnabled, DefaultOptions.temperatureUnit)
        legacyProfile.profile
    }
    return object : ProfileProvider<MiscProfile> {
        override val activeProfile: ProfileHolder<MiscProfile>
            get() = profileHolder
    }
}
