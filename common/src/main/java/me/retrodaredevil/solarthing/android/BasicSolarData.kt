package me.retrodaredevil.solarthing.android

import com.google.android.gms.wearable.DataMap

class BasicSolarData(
        val batteryVoltage: String,
        val acMode: Int?,
        val anyChargeControllerOn: Boolean,
        val batteryTemperatureString: String?,
        val dailyKWH: String,
        val pvKW: String,
        val loadKW: String
) {
    companion object {
        const val PATH = "/basic-solar"

        const val BATTERY_VOLTAGE = "batteryVoltage"
        const val AC_MODE = "acMode"
        const val ANY_CHARGE_CONTROLLER_ON = "anyChargeControllerOn"
        const val BATTERY_TEMPERATURE_STRING = "batteryTemperatureString"
        const val DAILY_KWH = "dailyKWH"
        const val PV_KW = "pvKW"
        const val LOAD_KW = "loadKW"

        fun fromDataMap(dataMap: DataMap): BasicSolarData {
            val batteryVoltage = dataMap.getString(BATTERY_VOLTAGE)
            val acMode = dataMap.getInt(AC_MODE, -1).let { if (it == -1) null else it }
            val anyCharging = dataMap.getBoolean(ANY_CHARGE_CONTROLLER_ON)
            val batteryTemperatureString = dataMap.getString(BATTERY_TEMPERATURE_STRING, null)
            val dailyKWH = dataMap.getString(DAILY_KWH)
            val pvKW = dataMap.getString(PV_KW)
            val loadKW = dataMap.getString(LOAD_KW)
            return BasicSolarData(batteryVoltage, acMode, anyCharging, batteryTemperatureString, dailyKWH, pvKW, loadKW)
        }
    }
    fun applyTo(dataMap: DataMap) {
        dataMap.apply {
            putString(BATTERY_VOLTAGE, batteryVoltage)
            if (acMode != null) {
                putInt(AC_MODE, acMode)
            }
            putBoolean(ANY_CHARGE_CONTROLLER_ON, anyChargeControllerOn)
            if (batteryTemperatureString != null) {
                putString(BATTERY_TEMPERATURE_STRING, batteryTemperatureString)
            }
            putString(DAILY_KWH, dailyKWH)
            putString(PV_KW, pvKW)
            putString(LOAD_KW, loadKW)
        }
    }
}
