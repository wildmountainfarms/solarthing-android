package me.retrodaredevil.solarthing.android.prefs

interface NetworkSwitchingProfile {

    var isEnabled: Boolean

    var isBackup: Boolean

    /**
     * If [isBackup] is true, the value of this has no meaning
     * @return The SSID of the WiFi network or null to represent being off WiFi (On a network)
     */
    var ssid: String?

}