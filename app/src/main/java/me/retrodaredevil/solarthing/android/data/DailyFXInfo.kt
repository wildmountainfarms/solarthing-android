package me.retrodaredevil.solarthing.android.data

class DailyFXInfo(
    val inverterKWH: Float,
    val buyKWH: Float,
    val chargerKWH: Float,
    val sellKWH: Float
) {
    operator fun plus(dailyFXInfo: DailyFXInfo): DailyFXInfo {
        return DailyFXInfo(
            inverterKWH + dailyFXInfo.inverterKWH,
            buyKWH + dailyFXInfo.buyKWH,
            chargerKWH + dailyFXInfo.chargerKWH,
            sellKWH + dailyFXInfo.sellKWH
        )
    }
}