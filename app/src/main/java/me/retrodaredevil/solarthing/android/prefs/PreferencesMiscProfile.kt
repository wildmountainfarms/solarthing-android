package me.retrodaredevil.solarthing.android.prefs

import android.content.SharedPreferences

class PreferencesMiscProfile(
    private val settings: SharedPreferences
) : MiscProfile {
    override var networkSwitchingEnabled: Boolean
        get() = settings.getBoolean(SaveKeys.networkSwitchingEnabled, DefaultOptions.networkSwitchingEnabled)
        set(value) = settings.edit().putBoolean(SaveKeys.networkSwitchingEnabled, value).apply()
    override var maxFragmentTimeMinutes: Float
        get() = settings.getFloat(
            SaveKeys.maxFragmentTimeMinutes,
            DefaultOptions.maxFragmentTimeMinutes
        )
        set(value) = settings.edit().putFloat(SaveKeys.maxFragmentTimeMinutes, value).apply()


    override var startOnBoot: Boolean
        get() = settings.getBoolean(
            SaveKeys.startOnBoot,
            DefaultOptions.startOnBoot
        )
        set(value) = settings.edit().putBoolean(SaveKeys.startOnBoot, value).apply()

}