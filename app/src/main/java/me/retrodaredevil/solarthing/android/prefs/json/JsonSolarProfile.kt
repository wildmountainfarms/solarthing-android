package me.retrodaredevil.solarthing.android.prefs.json

import me.retrodaredevil.solarthing.android.prefs.DefaultOptions
import me.retrodaredevil.solarthing.android.prefs.SaveKeys
import me.retrodaredevil.solarthing.android.prefs.SolarProfile

class JsonSolarProfile(
    private val jsonSaver: JsonSaver
) : SolarProfile {

    override var generatorFloatTimeHours: Float
        get() = jsonSaver.getAsFloat(SaveKeys.generatorFloatTimeHours) ?: DefaultOptions.generatorFloatTimeHours
        set(value) {
            jsonSaver[SaveKeys.generatorFloatTimeHours] = value
        }
    override var virtualFloatMinimumBatteryVoltage: Float?
        get() = jsonSaver.getAsFloat(SaveKeys.virtualFloatModeMinimumBatteryVoltage, DefaultOptions.virtualFloatModeMinimumBatteryVoltage)
        set(value) {
            jsonSaver[SaveKeys.virtualFloatModeMinimumBatteryVoltage] = value
        }
    override var lowBatteryVoltage: Float?
        get() = jsonSaver.getAsFloat(SaveKeys.lowBatteryVoltage, DefaultOptions.lowBatteryVoltage)
        set(value) {
            jsonSaver[SaveKeys.lowBatteryVoltage] = value
        }
    override var criticalBatteryVoltage: Float?
        get() = jsonSaver.getAsFloat(SaveKeys.criticalBatteryVoltage, DefaultOptions.criticalBatteryVoltage)
        set(value) {
            jsonSaver[SaveKeys.criticalBatteryVoltage] = value
        }

}