package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty

class MiscProfile(
    @JsonProperty(SaveKeys.maxFragmentTimeMinutes)
    val maxFragmentTimeMinutes: Float,
    @JsonProperty(SaveKeys.startOnBoot)
    val startOnBoot: Boolean,
    @JsonProperty(SaveKeys.networkSwitchingEnabled)
    val networkSwitchingEnabled: Boolean
)