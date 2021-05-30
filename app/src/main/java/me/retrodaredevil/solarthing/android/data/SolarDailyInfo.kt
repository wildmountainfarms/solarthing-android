package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.android.util.Formatting
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.solar.accumulation.AccumulationCalc
import me.retrodaredevil.solarthing.solar.accumulation.AccumulationConfig
import me.retrodaredevil.solarthing.solar.accumulation.AccumulationPair
import me.retrodaredevil.solarthing.solar.accumulation.AccumulationUtil
import me.retrodaredevil.solarthing.solar.accumulation.value.FloatAccumulationValue
import me.retrodaredevil.solarthing.solar.accumulation.value.FloatAccumulationValueFactory
import me.retrodaredevil.solarthing.solar.common.DailyChargeController
import me.retrodaredevil.solarthing.solar.common.DailyData
import me.retrodaredevil.solarthing.solar.outback.fx.charge.FXChargingPacket
import me.retrodaredevil.solarthing.solar.outback.fx.extra.DailyFXPacket
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket


/**
 * @param dayStartTimeMillis The date in millis representing the beginning of the day
 * @param packetGroups The packet groups where each packet group is on or after the day start
 */
fun createSolarDailyInfo(dayStartTimeMillis: Long, packetGroups: List<FragmentedPacketGroup>, fxChargingPacket: FXChargingPacket?): SolarDailyInfo {
    val accumulationConfig = AccumulationConfig.createDefault(dayStartTimeMillis)

    val mxMap = AccumulationUtil.getAccumulationPairs(AccumulationUtil.mapPackets(MXStatusPacket::class.java, packetGroups), accumulationConfig)
    val roverMap = AccumulationUtil.getAccumulationPairs(AccumulationUtil.mapPackets(RoverStatusPacket::class.java, packetGroups), accumulationConfig)
    val dailyFXMap = AccumulationUtil.getAccumulationPairs(AccumulationUtil.mapPackets(DailyFXPacket::class.java, packetGroups), accumulationConfig)
    return SolarDailyInfo(
            dayStartTimeMillis,
            mxMap.mapValues { AccumulationCalc.getTotal(it.value, FloatAccumulationValue.convert(DailyChargeController::getDailyKWH), FloatAccumulationValueFactory.getInstance()).value }
                    + roverMap.mapValues { AccumulationCalc.getTotal(it.value, FloatAccumulationValue.convert(DailyChargeController::getDailyKWH), FloatAccumulationValueFactory.getInstance()).value },
            dailyFXMap.mapValues { getDailyFXTotal(it.value, ::DailyFXInfo) },
            fxChargingPacket
    )
}

class SolarDailyInfo(
        val dayStartTimeMillis: Long,
        val dailyKWHMap: Map<IdentifierFragment, Float>,
        val fxMap: Map<IdentifierFragment, DailyFXInfo>,
        val fxChargingPacket: FXChargingPacket?
) {
    val dailyFXInfo: DailyFXInfo? = if (fxMap.isEmpty()) null else fxMap.values.reduce { sum, element -> sum + element }

    val dailyKWH = dailyKWHMap.values.sum()
    val dailyKWHString = Formatting.TENTHS.format(dailyKWH)
}

fun <T> getDailyFXTotal(
        accumulationPairs: List<AccumulationPair<T>>,
        totalGetter: (T) -> DailyFXInfo
): DailyFXInfo where T : DailyData {
    var total = DailyFXInfo(0.0f, 0.0f, 0.0f, 0.0f)
    for (dailyPair in accumulationPairs) {
        total += if (dailyPair.startPacketType == AccumulationPair.StartPacketType.CUT_OFF) {
            totalGetter(dailyPair.latestPacket.packet) - totalGetter(dailyPair.startPacket.packet)
        } else {
            totalGetter(dailyPair.latestPacket.packet)
        }
    }
    return total
}
