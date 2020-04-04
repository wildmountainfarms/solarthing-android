package me.retrodaredevil.solarthing.android.prefs.saving

import android.content.SharedPreferences

class SharedPreferencesStringValueSaver(
    private val sharedPreferences: SharedPreferences,
    private val preferencesKey: String
) : StringValueSaver {
    override var stringValue: String?
        get() = sharedPreferences.getString(preferencesKey, null)
        set(value) {
//            println("New value would have been: $value")
            sharedPreferences.edit().putString(preferencesKey, value).apply()
        }
}
