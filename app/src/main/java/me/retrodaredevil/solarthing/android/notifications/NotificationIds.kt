/**
 * To avoid notifications being grouped, these IDs can be used for the id and group of a notification
 */
package me.retrodaredevil.solarthing.android.notifications

import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.solar.common.DailyData
import me.retrodaredevil.solarthing.solar.outback.OutbackData
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import java.util.*


const val PERSISTENT_NOTIFICATION_ID = 1
const val SOLAR_NOTIFICATION_ID = 2
const val GENERATOR_PERSISTENT_ID = 3
const val VOLTAGE_TIMER_NOTIFICATION_ID = 4
const val GENERATOR_DONE_NOTIFICATION_ID = 5
const val BATTERY_NOTIFICATION_ID = 6
const val END_OF_DAY_NOTIFICATION_ID = 7

const val END_OF_DAY_SUMMARY_ID = 9
const val DEVICE_CONNECTION_STATUS_SUMMARY_ID = 10
const val MORE_SOLAR_INFO_SUMMARY_ID = 11

const val END_OF_DAY_GROUP = "end_of_day"
const val DEVICE_CONNECTION_STATUS_GROUP = "connection"
const val MORE_SOLAR_INFO_GROUP = "more_solar_info"

fun getGroup(id: Int) = "group$id"

fun getEndOfDayInfoId(dailyData: DailyData): Int = when(dailyData){
    is MXStatusPacket -> Objects.hash("end_of_day", dailyData.address)
    is RoverStatusPacket -> Objects.hash("end_of_day", 10 + dailyData.productSerialNumber)
    else -> error("dailyData: $dailyData is not supported!")
}
fun getOutbackEndOfDayInfoId(packet: OutbackData): Int = Objects.hash("end_of_day", packet.address)

fun getDeviceConnectionStatusId(packet: Packet): Int = when(packet){
    is OutbackData -> Objects.hash("device_connection", packet.address)
    is RoverStatusPacket -> Objects.hash("device_connection", 10 + packet.productSerialNumber)
    else -> throw IllegalArgumentException("packet: $packet is not supported!")
}
fun getMoreSolarInfoId(packet: Packet): Int = when(packet){
    is OutbackData -> Objects.hash("more_solar_info", packet.address)
    is RoverStatusPacket -> Objects.hash("more_solar_info", 10 + packet.productSerialNumber)
    else -> throw IllegalArgumentException("$packet is not supported!")
}
fun getBatteryTemperatureId(packet: SolarStatusPacket) = when(packet){
    is RoverStatusPacket -> Objects.hash("battery_temperature_over", packet.productSerialNumber)
    else -> throw IllegalArgumentException("$packet is not supported!")
}
fun getControllerTemperatureId(packet: SolarStatusPacket) = when(packet){
    is RoverStatusPacket -> Objects.hash("controller_temperature_over", packet.productSerialNumber)
    else -> throw IllegalArgumentException("$packet is not supported!")
}
fun getDeviceCpuTemperatureId(fragmentId: Int?) = Objects.hash("device_cpu_temperature", fragmentId)
