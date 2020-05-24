package me.retrodaredevil.solarthing.android

import com.google.android.gms.wearable.DataMap

class BasicSolarData(
    val dateMillis: Long,
    val batteryVoltage: Float,
    val acMode: Int?,
    val anyChargeControllerOn: Boolean,
    val batteryTemperatureString: String?,
    val dailyKWH: Float,
    val pvKW: Float,
    val loadKW: Float
) {
    companion object {
        const val PATH = "/basic-solar"

        const val DATE_MILLIS = "dateMillis"
        const val BATTERY_VOLTAGE = "batteryVoltage"
        const val AC_MODE = "acMode"
        const val ANY_CHARGE_CONTROLLER_ON = "anyChargeControllerOn"
        const val BATTERY_TEMPERATURE_STRING = "batteryTemperatureString"
        const val DAILY_KWH = "dailyKWH"
        const val PV_KW = "pvKW"
        const val LOAD_KW = "loadKW"

        fun fromDataMap(dataMap: DataMap): BasicSolarData {
            val dateMillis = dataMap.getLong(DATE_MILLIS)
            val batteryVoltage = dataMap.getFloat(BATTERY_VOLTAGE)
            val acMode = dataMap.getInt(AC_MODE, -1).let { if (it == -1) null else it }
            val anyCharging = dataMap.getBoolean(ANY_CHARGE_CONTROLLER_ON)
            val batteryTemperatureString = dataMap.getString(BATTERY_TEMPERATURE_STRING, null)
            val dailyKWH = dataMap.getFloat(DAILY_KWH)
            val pvKW = dataMap.getFloat(PV_KW)
            val loadKW = dataMap.getFloat(LOAD_KW)
            return BasicSolarData(dateMillis, batteryVoltage, acMode, anyCharging, batteryTemperatureString, dailyKWH, pvKW, loadKW)
        }
    }
    fun applyTo(dataMap: DataMap) {
        dataMap.apply {
            putLong(DATE_MILLIS, dateMillis)
            putFloat(BATTERY_VOLTAGE, batteryVoltage)
            if (acMode != null) {
                putInt(AC_MODE, acMode)
            }
            putBoolean(ANY_CHARGE_CONTROLLER_ON, anyChargeControllerOn)
            if (batteryTemperatureString != null) {
                putString(BATTERY_TEMPERATURE_STRING, batteryTemperatureString)
            }
            putFloat(DAILY_KWH, dailyKWH)
            putFloat(PV_KW, pvKW)
            putFloat(LOAD_KW, loadKW)
        }
    }
    fun isOld(): Boolean {
        return dateMillis + 6 * 60 * 1000 < System.currentTimeMillis()
    }
}
