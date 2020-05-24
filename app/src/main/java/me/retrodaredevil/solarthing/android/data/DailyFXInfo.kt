package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.solar.outback.fx.extra.DailyFXPacket

class DailyFXInfo(
        val inverterKWH: Float,
        val buyKWH: Float,
        val chargerKWH: Float,
        val sellKWH: Float
) {
    constructor(dailyFXPacket: DailyFXPacket) : this(dailyFXPacket.inverterKWH, dailyFXPacket.buyKWH, dailyFXPacket.chargerKWH, dailyFXPacket.sellKWH)
    operator fun plus(dailyFXInfo: DailyFXInfo): DailyFXInfo {
        return DailyFXInfo(
                inverterKWH + dailyFXInfo.inverterKWH,
                buyKWH + dailyFXInfo.buyKWH,
                chargerKWH + dailyFXInfo.chargerKWH,
                sellKWH + dailyFXInfo.sellKWH
        )
    }
    operator fun minus(dailyFXInfo: DailyFXInfo): DailyFXInfo {
        return DailyFXInfo(
                inverterKWH - dailyFXInfo.inverterKWH,
                buyKWH - dailyFXInfo.buyKWH,
                chargerKWH - dailyFXInfo.chargerKWH,
                sellKWH - dailyFXInfo.sellKWH
        )
    }
}