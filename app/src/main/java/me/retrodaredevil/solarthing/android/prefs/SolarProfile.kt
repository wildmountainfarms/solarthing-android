package me.retrodaredevil.solarthing.android.prefs

interface SolarProfile {
    var voltageTimerTimeHours: Float
    var voltageTimerBatteryVoltage: Float?

    var lowBatteryVoltage: Float?
    var criticalBatteryVoltage: Float?

    var batteryVoltageType: BatteryVoltageType
}
enum class BatteryVoltageType(
    val saveValue: Int
) {
    AVERAGE(0),
    FIRST_PACKET(1),
    MOST_RECENT(2),
    FIRST_OUTBACK(3),
    FIRST_OUTBACK_FX(4);
    companion object {
        fun getType(saveValue: Int): BatteryVoltageType {
            for(type in values()){
                if(type.saveValue == saveValue){
                    return type
                }
            }
            throw IllegalArgumentException("No saveValue: $saveValue")
        }
    }
}
