/**
 * To avoid notifications being grouped, these IDs can be used for the id and group of a notification
 */
package me.retrodaredevil.solarthing.android.notifications

import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.solar.common.DailyData
import me.retrodaredevil.solarthing.solar.outback.OutbackPacket
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket


const val PERSISTENT_NOTIFICATION_ID = 1
const val SOLAR_NOTIFICATION_ID = 2
const val GENERATOR_PERSISTENT_ID = 3
const val GENERATOR_FLOAT_NOTIFICATION_ID = 4
const val GENERATOR_DONE_NOTIFICATION_ID = 5
const val BATTERY_NOTIFICATION_ID = 6
const val OUTHOUSE_NOTIFICATION_ID = 7
const val VACANT_NOTIFICATION_ID = 8

const val END_OF_DAY_SUMMARY_ID = 9
const val DEVICE_CONNECTION_STATUS_SUMMARY_ID = 10

const val END_OF_DAY_GROUP = "end_of_day"
const val DEVICE_CONNECTION_STATUS_GROUP = "connection"

fun getGroup(id: Int) = "group$id"

fun getEndOfDayInfoID(dailyData: DailyData): Int = when(dailyData){
    is MXStatusPacket -> 100 + dailyData.address
    is RoverStatusPacket -> 110 + dailyData.productSerialNumber
    else -> error("dailyData: $dailyData is not supported!")
}
fun getDeviceConnectionStatusID(packet: Packet): Int = when(packet){
    is OutbackPacket -> 200 + packet.address
    is RoverStatusPacket -> 210 + packet.productSerialNumber
    else -> error("packet: $packet is not supported!")
}
