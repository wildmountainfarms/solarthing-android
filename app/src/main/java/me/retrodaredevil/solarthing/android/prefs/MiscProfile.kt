package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty
import me.retrodaredevil.solarthing.android.util.TemperatureUnit

data class MiscProfile(
    @JsonProperty(SaveKeys.maxFragmentTimeMinutes, required = true)
    val maxFragmentTimeMinutes: Float,
    @JsonProperty(SaveKeys.startOnBoot, required = true)
    val startOnBoot: Boolean,
    @JsonProperty(SaveKeys.networkSwitchingEnabled, required = true)
    val networkSwitchingEnabled: Boolean,
    @JsonProperty(SaveKeys.temperatureUnit, required = true)
    val temperatureUnit: TemperatureUnit
)