package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty

class NetworkSwitchingProfile(
    @JsonProperty(SaveKeys.NetworkSwitching.isEnabled)
    val isEnabled: Boolean,
    @JsonProperty(SaveKeys.NetworkSwitching.isBackup)
    val isBackup: Boolean,
    /**
     * If [isBackup] is true, the value of this has no meaning
     * @return The SSID of the WiFi network or null to represent being off WiFi (On a network)
     */
    @JsonProperty(SaveKeys.NetworkSwitching.ssid)
    val ssid: String?
)