package me.retrodaredevil.solarthing.android.prefs.json

import com.google.gson.JsonElement
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

    operator fun set(key: String, value: Number?){
        reloadedJsonObject.addProperty(key, value)
        save()
    }
    operator fun set(key: String, value: Boolean?){
        reloadedJsonObject.addProperty(key, value)
        save()
    }
    operator fun set(key: String, value: String?){
        reloadedJsonObject.addProperty(key, value)
        save()
    }
    operator fun set(key: String, value: Char?){
        reloadedJsonObject.addProperty(key, value)
        save()
    }
    fun remove(key: String){
        reloadedJsonObject.remove(key)
        save()
    }

    private fun <T> get(key: String, getter: (JsonElement) -> T, defaultValue: T?): T?{
        val r = reloadedJsonObject[key]
        if(r?.isJsonNull == true){ // The property was null but not undefined
            return null
        }
        if(r != null){
            return getter(r)
        }
        return defaultValue // The property was undefined
    }
    fun getAsNumber(key: String, defaultValue: Number? = null): Number?{
        return get(key, JsonElement::getAsNumber, defaultValue)
    }
    fun getAsInt(key: String, defaultValue: Int? = null): Int? {
        return get(key, JsonElement::getAsInt, defaultValue)
    }
    fun getAsFloat(key: String, defaultValue: Float? = null): Float? {
        return get(key, JsonElement::getAsFloat, defaultValue)
    }
    fun getAsBoolean(key: String, defaultValue: Boolean? = null): Boolean?{
        return get(key, JsonElement::getAsBoolean, defaultValue)
    }
    fun getAsString(key: String, defaultValue: String? = null): String?{
        return get(key, JsonElement::getAsString, defaultValue)
    }

}