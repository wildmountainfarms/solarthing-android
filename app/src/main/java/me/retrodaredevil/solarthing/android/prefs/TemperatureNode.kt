package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty

data class TemperatureNode(
    @JsonProperty("notify_battery", required = true)
    val battery: Boolean = false,
    @JsonProperty("notify_controller", required = true)
    val controller: Boolean = false,
    @JsonProperty("notify_device_cpu", required = true)
    val deviceCpu: Boolean = false,
    @JsonProperty("device_cpu_ids", required = true)
    val deviceCpuIds: List<Int> = emptyList(),
    @JsonProperty("high_threshold_celsius", required = true)
    val highThresholdCelsius: Float? = null,
    @JsonProperty("low_threshold_celsius", required = true)
    val lowThresholdCelsius: Float? = null,
    @JsonProperty("is_critical", required = true)
    val isCritical: Boolean = false
)
