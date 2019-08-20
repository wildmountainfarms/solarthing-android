package me.retrodaredevil.solarthing.android.prefs.json

import com.google.gson.JsonObject

interface JsonSaver {
    val jsonObject: JsonObject
    fun reload()
    fun save()

    val reloadedJsonObject: JsonObject
        get() {
            reload()
            return jsonObject
        }
}