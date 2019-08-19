package me.retrodaredevil.solarthing.android.prefs.json

import com.google.gson.JsonObject

class NestedJsonSaver(
    private val jsonObjectGetter: () -> JsonObject,
    private val doApply: () -> Unit
) : JsonSaver {

    constructor(jsonSaver: JsonSaver, propertyName: String) :
        this(
            {jsonSaver.jsonObject.getAsJsonObject(propertyName)},
            jsonSaver::save
        )

    override val jsonObject: JsonObject
        get() = jsonObjectGetter()

    override fun save() {
        doApply()
    }

}