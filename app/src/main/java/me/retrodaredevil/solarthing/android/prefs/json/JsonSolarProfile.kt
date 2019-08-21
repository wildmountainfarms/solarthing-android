package me.retrodaredevil.solarthing.android.prefs.json

import me.retrodaredevil.solarthing.android.prefs.SaveKeys
import me.retrodaredevil.solarthing.android.prefs.SolarProfile

class JsonSolarProfile(
    private val jsonSaver: JsonSaver
) : SolarProfile {

    override var generatorFloatTimeHours: Float
        get() = jsonSaver.reloadedJsonObject[SaveKeys.generatorFloatTimeHours].asFloat
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.generatorFloatTimeHours, value)
            jsonSaver.save()
        }
    override var virtualFloatMinimumBatteryVoltage: Float?
        get() {
            val value = jsonSaver.reloadedJsonObject[SaveKeys.virtualFloatModeMinimumBatteryVoltage]
            return if(value.isJsonNull) null else value.asFloat
        }
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.virtualFloatModeMinimumBatteryVoltage, value)
            jsonSaver.save()
        }
    override var lowBatteryVoltage: Float?
        get() {
            val value = jsonSaver.reloadedJsonObject[SaveKeys.lowBatteryVoltage]
            return if(value.isJsonNull) null else value.asFloat
        }
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.lowBatteryVoltage, value)
            jsonSaver.save()
        }
    override var criticalBatteryVoltage: Float?
        get() {
            val value = jsonSaver.reloadedJsonObject[SaveKeys.criticalBatteryVoltage]
            return if(value.isJsonNull) null else value.asFloat
        }
        set(value) {
            jsonSaver.jsonObject.addProperty(SaveKeys.criticalBatteryVoltage, value)
            jsonSaver.save()
        }

}