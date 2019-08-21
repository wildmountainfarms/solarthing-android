package me.retrodaredevil.solarthing.android.prefs

interface SolarProfile {
    var generatorFloatTimeHours: Float
    var virtualFloatMinimumBatteryVoltage: Float?

    var lowBatteryVoltage: Float?
    var criticalBatteryVoltage: Float?
}