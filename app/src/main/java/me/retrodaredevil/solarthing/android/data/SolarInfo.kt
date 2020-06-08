package me.retrodaredevil.solarthing.android.data

class SolarInfo(
        val solarPacketInfo: SolarPacketInfo,
        private val solarDailyInfoGetter: () -> SolarDailyInfo
) {
    val solarDailyInfo: SolarDailyInfo by lazy { solarDailyInfoGetter() }
}
