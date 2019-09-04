package me.retrodaredevil.solarthing.android.prefs

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat

/**
 * @param settings The shared preferences instance
 * @param context The context used to check if coarse location is accessible or null to prevent that check
 */
class PreferencesMiscProfile(
    private val settings: SharedPreferences,
    private val context: Context?
) : MiscProfile {
    override var networkSwitchingEnabled: Boolean
        get() {
            if(context != null){
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    return false
                }
            }
            return settings.getBoolean(SaveKeys.networkSwitchingEnabled, DefaultOptions.networkSwitchingEnabled)
        }
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