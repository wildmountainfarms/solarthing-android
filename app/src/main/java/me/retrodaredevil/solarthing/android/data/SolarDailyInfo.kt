package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment

fun createSolarDailyInfo(packetGroups: List<PacketGroup>): SolarDailyInfo {
    TODO("Still have to implement")
}

class SolarDailyInfo(
    val dailyKWHMap: Map<IdentifierFragment, Float>,
    val fxMap: Map<IdentifierFragment, DailyFXInfo>
) {
    val dailyFXInfo: DailyFXInfo? = if (fxMap.isEmpty()) null else fxMap.values.reduce { sum, element -> sum + element }
}
