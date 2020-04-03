package me.retrodaredevil.solarthing.android.prefs.gson

import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

@Deprecated("This is part of the old prefs code")
class PreferencesJsonSaver(
    private val sharedPreferences: SharedPreferences,
    private val preferencesKey: String,
    private val newJsonObjectCreator: () -> JsonObject
) : JsonSaver {
    companion object {
        private val GSON = GsonBuilder().create()
    }
    private var cachedJsonObject: JsonObject

    init {
        cachedJsonObject = getCurrentJsonObject()
    }
    private fun getCurrentJsonObject(): JsonObject{
        val jsonString = sharedPreferences.getString(preferencesKey, null)
        return if(jsonString == null){
            val r = newJsonObjectCreator()
            saveJsonObject(r)
            r
        } else {
            GSON.fromJson(jsonString, JsonObject::class.java)
        }
    }
    private fun saveJsonObject(jsonObject: JsonObject){
        sharedPreferences.edit().putString(preferencesKey, jsonObject.toString()).apply()
    }

    override val jsonObject: JsonObject
        get() = cachedJsonObject

    override fun reload() {
        cachedJsonObject = getCurrentJsonObject()
    }
    override fun save() {
        saveJsonObject(cachedJsonObject)
    }

}