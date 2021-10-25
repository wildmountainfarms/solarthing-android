/**
 * To avoid notifications being grouped, these IDs can be used for the id and group of a notification
 */
package me.retrodaredevil.solarthing.android.notifications

import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.solar.batteryvoltage.BatteryVoltageOnlyPacket
import me.retrodaredevil.solarthing.solar.outback.OutbackData
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import me.retrodaredevil.solarthing.solar.tracer.TracerStatusPacket
import org.slf4j.LoggerFactory
import java.util.*


const val PERSISTENT_NOTIFICATION_ID = 1
const val SOLAR_NOTIFICATION_ID = 2
const val GENERATOR_PERSISTENT_ID = 3
const val VOLTAGE_TIMER_NOTIFICATION_ID = 4
const val GENERATOR_DONE_NOTIFICATION_ID = 5
const val BATTERY_NOTIFICATION_ID = 6
const val END_OF_DAY_NOTIFICATION_ID = 7

const val DEVICE_CONNECTION_STATUS_SUMMARY_ID = 10
const val MORE_SOLAR_INFO_SUMMARY_ID = 11

const val SCHEDULED_COMMAND_NOTIFICATION_START_ID = 100 // 100..109
const val SCHEDULED_COMMAND_MAX_NOTIFICATIONS = 10
const val CANCEL_SCHEDULED_COMMAND_SERVICE_NOTIFICATION_ID = 110
const val CANCEL_SCHEDULED_COMMAND_RESULT_NOTIFICATION_ID = 111
const val CANCEL_SCHEDULED_COMMAND_ALREADY_RUNNING_NOTIFICATION_ID = 112
const val CANCEL_SCHEDULED_COMMAND_NO_GENERATED_KEY_NOTIFICATION_ID = 113

const val DEVICE_CONNECTION_STATUS_GROUP = "connection"
const val MORE_SOLAR_INFO_GROUP = "more_solar_info"

private class NotificationIds
private val LOGGER = LoggerFactory.getLogger(NotificationIds::class.java)

fun getGroup(id: Int) = "group$id"

fun getDeviceConnectionStatusId(packet: Packet): Int = when(packet){
    is OutbackData -> Objects.hash("device_connection", packet.address)
    is RoverStatusPacket -> Objects.hash("device_connection", 10 + packet.productSerialNumber)
    is BatteryVoltageOnlyPacket -> Objects.hash("device_connection", packet.dataId, "battery_voltage_only")
    is TracerStatusPacket -> Objects.hash("device_connection", packet.ratedOutputCurrent, packet.ratedInputCurrent, packet.ratedLoadOutputCurrent, packet.ratedInputVoltage, "tracer")
    else -> {
        LOGGER.warn("Unsupported device: $packet. falling back to a random value")
        (Int.MIN_VALUE..Int.MAX_VALUE).random()
    }
}
fun getMoreSolarInfoId(packet: Packet): Int = when(packet){
    is OutbackData -> Objects.hash("more_solar_info", packet.address)
    is RoverStatusPacket -> Objects.hash("more_solar_info", 10 + packet.productSerialNumber)
    is TracerStatusPacket -> Objects.hash("more_solar_info", 200)
    else -> throw IllegalArgumentException("$packet is not supported!")
}
fun getBatteryTemperatureId(identifierFragment: IdentifierFragment) =
        Objects.hash("battery_temperature_over", identifierFragment.fragmentId, identifierFragment.identifier, identifierFragment.identifier.representation)
fun getControllerTemperatureId(identifierFragment: IdentifierFragment) =
        Objects.hash("controller_temperature_over", identifierFragment.fragmentId, identifierFragment.identifier, identifierFragment.identifier.representation)
fun getDeviceCpuTemperatureId(fragmentId: Int) = Objects.hash("device_cpu_temperature", fragmentId)
