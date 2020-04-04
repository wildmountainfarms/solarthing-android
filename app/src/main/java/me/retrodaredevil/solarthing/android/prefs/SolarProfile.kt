package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder

@JsonIgnoreProperties(
    "generator_float_hours", // we aren't using this anymore
    "virtual_float_mode_minimum_battery_voltage" // we aren't using this anymore
)
class SolarProfile
@JsonCreator
constructor(
    @get:JsonProperty()
    val voltageTimerNodes: List<VoltageTimerNode> = emptyList(),
    val temperatureNodes: List<TemperatureNode> = emptyList(),
    @get:JsonProperty(SaveKeys.lowBatteryVoltage)
    val lowBatteryVoltage: Float? = DefaultOptions.lowBatteryVoltage,
    @get:JsonProperty(SaveKeys.criticalBatteryVoltage)
    val criticalBatteryVoltage: Float? = DefaultOptions.criticalBatteryVoltage,
    @get:JsonProperty(SaveKeys.batteryVoltageType)
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
