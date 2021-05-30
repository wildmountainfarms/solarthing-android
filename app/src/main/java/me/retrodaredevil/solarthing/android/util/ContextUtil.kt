package me.retrodaredevil.solarthing.android.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log

fun Context.getDeviceProtectedStorageSharedPreferences(name: String, mode: Int): SharedPreferences {
    // thanks https://stackoverflow.com/a/55494418/5434860
    val deviceContext = this.createDeviceProtectedStorageContext()
    if (!deviceContext.moveSharedPreferencesFrom(this, name)) {
        Log.w("ContextUtils.kt", "Failed to migrate shared preferences.")
    }
    return deviceContext.getSharedPreferences(name, mode)
}
