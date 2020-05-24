package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.*

@JsonIgnoreProperties(
        "generator_float_hours", // we aren't using this anymore
        "virtual_float_mode_minimum_battery_voltage" // we aren't using this anymore
)
data class SolarProfile(
        @JsonAlias("voltageTimerNodes") // we can remove this soon
        @JsonProperty(SaveKeys.voltageTimerNodes)
        val voltageTimerNodes: List<VoltageTimerNode> = emptyList(),
        @JsonAlias("temperatureNodes") // we can remove this soon
        @JsonProperty(SaveKeys.temperatureNodes)
        val temperatureNodes: List<TemperatureNode> = emptyList(),
        @JsonProperty(SaveKeys.lowBatteryVoltage)
        val lowBatteryVoltage: Float? = DefaultOptions.lowBatteryVoltage,
        @JsonProperty(SaveKeys.criticalBatteryVoltage)
        val criticalBatteryVoltage: Float? = DefaultOptions.criticalBatteryVoltage,
        @JsonProperty(SaveKeys.batteryVoltageType)
        val batteryVoltageType: BatteryVoltageType = DefaultOptions.batteryVoltageType
)
enum class BatteryVoltageType(
        @get:JsonValue
        val saveValue: Int
) {
    AVERAGE(0),
    FIRST_PACKET(1),
    MOST_RECENT(2),
    FIRST_OUTBACK(3),
    FIRST_OUTBACK_FX(4);
    companion object {
        @JsonCreator
        @JvmStatic
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
