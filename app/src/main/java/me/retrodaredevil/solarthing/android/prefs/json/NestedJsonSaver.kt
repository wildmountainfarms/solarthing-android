package me.retrodaredevil.solarthing.android.prefs.json

import com.google.gson.JsonObject

class NestedJsonSaver(
    private val jsonObjectGetter: () -> JsonObject,
    private val doReload: () -> Unit,
    private val doApply: () -> Unit
) : JsonSaver {

    constructor(jsonSaver: JsonSaver, propertyName: String) :
        this(
            {
                val jsonObject = jsonSaver.jsonObject
                jsonObject.getAsJsonObject(propertyName) ?: error("There's no JsonObject with property name: $propertyName. parent object: $jsonObject")
            },
            jsonSaver::reload,
            jsonSaver::save
        )

    override val jsonObject: JsonObject
        get() = jsonObjectGetter()

    override fun reload() {
        doReload()
    }
    override fun save() {
        doApply()
    }

}