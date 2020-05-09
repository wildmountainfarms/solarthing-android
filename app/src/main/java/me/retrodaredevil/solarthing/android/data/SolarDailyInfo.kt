package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.android.util.Formatting
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.solar.common.DailyChargeController
import me.retrodaredevil.solarthing.solar.common.DailyData
import me.retrodaredevil.solarthing.solar.daily.DailyCalc
import me.retrodaredevil.solarthing.solar.daily.DailyConfig
import me.retrodaredevil.solarthing.solar.daily.DailyPair
import me.retrodaredevil.solarthing.solar.daily.DailyUtil
import me.retrodaredevil.solarthing.solar.outback.fx.extra.DailyFXPacket
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket


/**
 * @param dayStartTimeMillis The date in millis representing the beginning of the day
 * @param packetGroups The packet groups where each packet group is on or after the day start
 */
fun createSolarDailyInfo(dayStartTimeMillis: Long, packetGroups: List<FragmentedPacketGroup>): SolarDailyInfo {
    val dailyConfig = DailyConfig(dayStartTimeMillis + 3 * 60 * 60 * 1000, dayStartTimeMillis + 10 * 60 * 60 * 1000)

    val mxMap = DailyUtil.getDailyPairs(DailyUtil.mapPackets(MXStatusPacket::class.java, packetGroups), dailyConfig)
    val roverMap = DailyUtil.getDailyPairs(DailyUtil.mapPackets(RoverStatusPacket::class.java, packetGroups), dailyConfig)
    val dailyFXMap = DailyUtil.getDailyPairs(DailyUtil.mapPackets(DailyFXPacket::class.java, packetGroups), dailyConfig)
    return SolarDailyInfo(
        dayStartTimeMillis,
        mxMap.mapValues { DailyCalc.getTotal(it.value, DailyChargeController::getDailyKWH) } + roverMap.mapValues { DailyCalc.getTotal(it.value, DailyChargeController::getDailyKWH) },
        dailyFXMap.mapValues { getDailyFXTotal(it.value, ::DailyFXInfo) }
    )
}

class SolarDailyInfo(
    val dayStartTimeMillis: Long,
    val dailyKWHMap: Map<IdentifierFragment, Float>,
    val fxMap: Map<IdentifierFragment, DailyFXInfo>
) {
    val dailyFXInfo: DailyFXInfo? = if (fxMap.isEmpty()) null else fxMap.values.reduce { sum, element -> sum + element }

    val dailyKWH = dailyKWHMap.values.sum()
    val dailyKWHString = Formatting.TENTHS.format(dailyKWH)
}

fun <T> getDailyFXTotal(
    dailyPairs: List<DailyPair<T>>,
    totalGetter: (T) -> DailyFXInfo
): DailyFXInfo where T : DailyData {
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
