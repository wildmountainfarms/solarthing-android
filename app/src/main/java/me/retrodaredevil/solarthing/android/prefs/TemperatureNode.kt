package me.retrodaredevil.solarthing.android.prefs

data class TemperatureNode(
    val battery: Boolean = false,
    val controller: Boolean = false,
    val deviceCpu: Boolean = false,
    val lowThresholdCelsius: Float? = null,
    val highThresholdCelsius: Float? = null
)
