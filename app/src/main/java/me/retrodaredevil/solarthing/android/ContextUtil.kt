package me.retrodaredevil.solarthing.android

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log

fun Context.getDeviceProtectedStorageSharedPreferences(name: String, mode: Int): SharedPreferences =
    // thanks https://stackoverflow.com/a/55494418/5434860
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
        val deviceContext = this.createDeviceProtectedStorageContext()
        if(!deviceContext.moveSharedPreferencesFrom(this, name)){
            Log.w("ContextUtils.kt", "Failed to migrate shared preferences.")
        }
        deviceContext
    } else {
        this
    }.getSharedPreferences(name, mode)
