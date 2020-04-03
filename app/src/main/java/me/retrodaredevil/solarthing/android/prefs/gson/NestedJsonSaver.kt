package me.retrodaredevil.solarthing.android.prefs.gson

import com.google.gson.JsonObject

@Deprecated("This is part of the old prefs code")
class NestedJsonSaver(
    private val jsonObjectGetter: () -> JsonObject,
    private val doReload: () -> Unit,
    private val doApply: () -> Unit
) : JsonSaver {

    constructor(jsonSaver: JsonSaver, propertyName: String, setIfNull: Boolean = true) :
        this(
            {
                val jsonObject = jsonSaver.reloadedJsonObject
                var r = jsonObject.getAsJsonObject(propertyName)
                if(r == null){
                    if(setIfNull){
                        r = JsonObject()
                        jsonObject.add(propertyName, r)
                        jsonSaver.save()
                    } else {
                        error("There's no JsonObject with property name: $propertyName. parent object: $jsonObject")
                    }
                }
                r
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