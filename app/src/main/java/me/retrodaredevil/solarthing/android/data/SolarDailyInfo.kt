package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.solar.common.DailyChargeController
import me.retrodaredevil.solarthing.solar.common.DailyData
import me.retrodaredevil.solarthing.solar.daily.DailyConfig
import me.retrodaredevil.solarthing.solar.daily.DailyPair
import me.retrodaredevil.solarthing.solar.daily.DailyUtil
import me.retrodaredevil.solarthing.solar.outback.fx.extra.DailyFXPacket
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket


fun createSolarDailyInfo(dayStartTimeMillis: Long, packetGroups: List<FragmentedPacketGroup>): SolarDailyInfo {
    val dailyConfig = DailyConfig(dayStartTimeMillis + 3 * 60 * 60 * 1000, dayStartTimeMillis + 10 * 60 * 60 * 1000)

    val mxMap = DailyUtil.getDailyPairs(DailyUtil.mapPackets(MXStatusPacket::class.java, packetGroups), dailyConfig)
    val roverMap = DailyUtil.getDailyPairs(DailyUtil.mapPackets(RoverStatusPacket::class.java, packetGroups), dailyConfig)
    val dailyFXMap = DailyUtil.getDailyPairs(DailyUtil.mapPackets(DailyFXPacket::class.java, packetGroups), dailyConfig)
    return SolarDailyInfo(
        mxMap.mapValues { getTotal(it.value, DailyChargeController::getDailyKWH) } + roverMap.mapValues { getTotal(it.value, DailyChargeController::getDailyKWH) },
        dailyFXMap.mapValues { getDailyFXTotal(it.value, ::DailyFXInfo) }
    )
}

class SolarDailyInfo(
    val dailyKWHMap: Map<IdentifierFragment, Float>,
    val fxMap: Map<IdentifierFragment, DailyFXInfo>
) {
    val dailyFXInfo: DailyFXInfo? = if (fxMap.isEmpty()) null else fxMap.values.reduce { sum, element -> sum + element }
}

fun <T> getTotal(
    dailyPairs: List<DailyPair<T>>,
    totalGetter: (T) -> Float
): Float where T : Packet, T : DailyData {
    var total = 0f
    for (dailyPair in dailyPairs) {
        total += if (dailyPair.startPacketType == DailyPair.StartPacketType.CUT_OFF) {
            totalGetter(dailyPair.latestPacket.packet) - totalGetter(dailyPair.startPacket.packet)
        } else {
            totalGetter(dailyPair.latestPacket.packet)
        }
    }
    return total
}
fun <T> getDailyFXTotal(
    dailyPairs: List<DailyPair<T>>,
    totalGetter: (T) -> DailyFXInfo
): DailyFXInfo where T : Packet, T : DailyData {
    var total = DailyFXInfo(0.0f, 0.0f, 0.0f, 0.0f)
    for (dailyPair in dailyPairs) {
        total += if (dailyPair.startPacketType == DailyPair.StartPacketType.CUT_OFF) {
            totalGetter(dailyPair.latestPacket.packet) - totalGetter(dailyPair.startPacket.packet)
        } else {
            totalGetter(dailyPair.latestPacket.packet)
        }
    }
    return total
}
