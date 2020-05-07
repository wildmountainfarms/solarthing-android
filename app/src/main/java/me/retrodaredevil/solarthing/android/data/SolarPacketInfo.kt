package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.android.util.Formatting
import me.retrodaredevil.solarthing.android.prefs.BatteryVoltageType
import me.retrodaredevil.solarthing.misc.device.CpuTemperaturePacket
import me.retrodaredevil.solarthing.misc.device.DevicePacket
import me.retrodaredevil.solarthing.misc.device.DevicePacketType
import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.solar.SolarStatusPacketType
import me.retrodaredevil.solarthing.solar.common.BasicChargeController
import me.retrodaredevil.solarthing.solar.common.BatteryVoltage
import me.retrodaredevil.solarthing.solar.common.DailyChargeController
import me.retrodaredevil.solarthing.solar.extra.SolarExtraPacket
import me.retrodaredevil.solarthing.solar.extra.SolarExtraPacketType
import me.retrodaredevil.solarthing.solar.outback.OutbackData
import me.retrodaredevil.solarthing.solar.outback.OutbackUtil
import me.retrodaredevil.solarthing.solar.outback.fx.*
import me.retrodaredevil.solarthing.solar.outback.fx.charge.FXChargingPacket
import me.retrodaredevil.solarthing.solar.outback.fx.extra.DailyFXPacket
import me.retrodaredevil.solarthing.solar.outback.mx.MXErrorMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.outback.mx.extra.DailyMXPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverErrorMode
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverIdentifier
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import kotlin.math.round

class DailyFXInfo(
    val inverterKWH: Float,
    val buyKWH: Float,
    val chargerKWH: Float,
    val sellKWH: Float
)

/**
 * A class that deals with making a [PacketGroup] with solar data easier to retrieve values from
 */
class SolarPacketInfo(
    val packetGroup: FragmentedPacketGroup,
    batteryVoltageType: BatteryVoltageType
) {
    val dateMillis = packetGroup.dateMillis


    /** A map of the port number to the FX status packet associated with that device*/
    val fxMap: Map<IdentifierFragment, FXStatusPacket>
    /** A map of the port number to the MX/FM status packet associated with device*/
    val mxMap: Map<IdentifierFragment, MXStatusPacket>
    val roverMap: Map<RoverIdentifier, RoverStatusPacket> // TODO change key to IdentifierFragment

    val deviceMap: Map<IdentifierFragment, SolarStatusPacket>
    val basicChargeControllerMap: Map<IdentifierFragment, BasicChargeController>
    val rawDailyChargeControllerMap: Map<IdentifierFragment, DailyChargeController>
    val dailyChargeControllerMap: Map<IdentifierFragment, DailyChargeController>
    val batteryMap: Map<IdentifierFragment, BatteryVoltage>

    val dailyFXMap: Map<IdentifierFragment, DailyFXPacket>
    val dailyMXMap: Map<IdentifierFragment, DailyMXPacket>

    val masterFXStatusPacket: FXStatusPacket?
    val fxChargingPacket: FXChargingPacket?

    /** The battery voltage */
    val batteryVoltage: Float
    /** The battery voltage as a string (in base 10)*/
    val batteryVoltageString: String

    val estimatedBatteryVoltage: Float
    val estimatedBatteryVoltageString: String

    /** The PV Wattage (Power from the solar panels)*/
    val pvWattage: Int
    /** The amount of power from the solar panels that is charging the battery*/
    val pvChargerWattage: Float
    /** The load from the FX's */
    val load: Int
    /** The power from the generator going into the batteries*/
    val generatorToBatteryWattage: Int
    /** The total power from the generator */
    val generatorTotalWattage: Int

    val dailyFXInfo: DailyFXInfo?

    /**
     * The AC Mode which can be used to determine the state of the generator
     */
    val acMode: ACMode
    val generatorChargingBatteries: Boolean

    val dailyKWHours: Float

    val warningsCount: Int
    val hasWarnings: Boolean
    val errorsCount: Int

    val deviceCpuTemperatureMap: Map<Int?, Float>

    init {
        fxMap = LinkedHashMap()
        mxMap = LinkedHashMap()
        roverMap = LinkedHashMap()
        deviceMap = LinkedHashMap()
        basicChargeControllerMap = LinkedHashMap()
        rawDailyChargeControllerMap = LinkedHashMap()
        dailyChargeControllerMap = LinkedHashMap()
        batteryMap = LinkedHashMap()
        dailyFXMap = LinkedHashMap()
        dailyMXMap = LinkedHashMap()
        deviceCpuTemperatureMap = LinkedHashMap()
        var fxChargingPacket: FXChargingPacket? = null
        for(packet in packetGroup.packets){
            if(packet is SolarStatusPacket){
                val identifierFragment = IdentifierFragment(packetGroup.getFragmentId(packet), packet.identifier)
                deviceMap[identifierFragment] = packet
                when(packet.packetType){
                    SolarStatusPacketType.FX_STATUS -> {
                        val fx = packet as FXStatusPacket
                        fxMap[identifierFragment] = fx
                        batteryMap[identifierFragment] = fx
                    }
                    SolarStatusPacketType.MXFM_STATUS -> {
                        val mx = packet as MXStatusPacket
                        mxMap[identifierFragment] = mx
                        batteryMap[identifierFragment] = mx
                        basicChargeControllerMap[identifierFragment] = mx
                        rawDailyChargeControllerMap[identifierFragment] = mx
                        (dailyChargeControllerMap as MutableMap).getOrPut(identifierFragment) { mx }
                    }
                    SolarStatusPacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
                    SolarStatusPacketType.RENOGY_ROVER_STATUS -> {
                        val rover = packet as RoverStatusPacket
                        roverMap[rover.identifier] = rover
                        batteryMap[identifierFragment] = rover
                        basicChargeControllerMap[identifierFragment] = rover
                        rawDailyChargeControllerMap[identifierFragment] = rover
                        dailyChargeControllerMap[identifierFragment] = rover
                    }
                    null -> throw NullPointerException("packetType is null! packet: $packet")
                    else -> System.err.println("Unknown packet type: ${packet.packetType}")
                }
            } else if(packet is SolarExtraPacket){
                val fragmentId = packetGroup.getFragmentId(packet)
                when(packet.packetType){
                    SolarExtraPacketType.FX_DAILY -> {
                        packet as DailyFXPacket
                        dailyFXMap[IdentifierFragment(fragmentId, packet.identifier.supplementaryTo)] = packet
                    }
                    SolarExtraPacketType.MXFM_DAILY -> {
                        packet as DailyMXPacket
                        dailyMXMap[IdentifierFragment(fragmentId, packet.identifier)] = packet
                        dailyChargeControllerMap[IdentifierFragment(fragmentId, packet.identifier.supplementaryTo)] = packet
                    }
                    SolarExtraPacketType.FX_CHARGING -> {
                        packet as FXChargingPacket
                        fxChargingPacket = packet
                    }
                    null -> throw NullPointerException("packetType is null! packet: $packet")
                    else -> System.err.println("Unimplemented packet type: ${packet.packetType}")
                }
            } else if(packet is DevicePacket){
                when(packet.packetType){
                    DevicePacketType.DEVICE_CPU_TEMPERATURE -> {
                        packet as CpuTemperaturePacket
                        deviceCpuTemperatureMap[packetGroup.getFragmentId(packet)] = packet.cpuTemperatureCelsius
                    }
                    null -> throw NullPointerException()
                    else -> System.err.println("Unimplemented device packet type: ${packet.packetType}")
                }
            }
        }
        masterFXStatusPacket = OutbackUtil.getMasterFX(fxMap.values) // TODO there may be a better way to indicate the master in the main solarthing codebase
        this.fxChargingPacket = fxChargingPacket
        batteryVoltage = when(batteryVoltageType){
            BatteryVoltageType.AVERAGE -> batteryMap.values.let { it.sumByDouble { packet -> packet.batteryVoltage.toDouble() } / it.size }.toFloat()
            BatteryVoltageType.FIRST_PACKET -> batteryMap.values.first().batteryVoltage
            BatteryVoltageType.MOST_RECENT -> batteryMap.values.maxBy { packetGroup.getDateMillis(it as Packet) }!!.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK -> batteryMap.values.firstOrNull { it is OutbackData }?.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK_FX -> fxMap.values.firstOrNull()?.batteryVoltage
        } ?: batteryMap.values.first().batteryVoltage
        batteryVoltageString = Formatting.TENTHS.format(batteryVoltage)

        estimatedBatteryVoltage = (round(batteryMap.values.sumByDouble { it.batteryVoltage.toDouble() } / batteryMap.size * 10) / 10).toFloat()
        estimatedBatteryVoltageString = Formatting.FORMAT.format(estimatedBatteryVoltage)

        acMode = if(fxMap.isNotEmpty()) fxMap.values.first().acMode else ACMode.NO_AC
        generatorChargingBatteries = if(masterFXStatusPacket == null) false else masterFXStatusPacket.operationalMode in setOf(OperationalMode.CHARGE, OperationalMode.FLOAT, OperationalMode.EQ)
        load = fxMap.values.sumBy { it.inverterWattage }
        generatorToBatteryWattage = fxMap.values.sumBy { it.chargerWattage }
        generatorTotalWattage = fxMap.values.sumBy { it.buyWattage }

        pvWattage = basicChargeControllerMap.values.sumByDouble { it.pvCurrent.toDouble() * it.inputVoltage.toDouble() }.toInt()
        pvChargerWattage = basicChargeControllerMap.values.sumByDouble { it.chargingPower.toDouble() }.toFloat()
        dailyKWHours = dailyChargeControllerMap.values.map { it.dailyKWH }.sum()

        dailyFXInfo = if(dailyFXMap.isEmpty()){
            null
        } else {
            DailyFXInfo(dailyFXMap.values.sumByDouble { it.inverterKWH.toDouble() }.toFloat(),
                    dailyFXMap.values.sumByDouble { it.buyKWH.toDouble() }.toFloat(),
                    dailyFXMap.values.sumByDouble { it.chargerKWH.toDouble() }.toFloat(),
                    dailyFXMap.values.sumByDouble { it.inverterKWH.toDouble() }.toFloat()
            )
        }

        warningsCount = WarningMode.values().count { warningMode -> fxMap.values.any { warningMode.isActive(it.warningModeValue) } }
        hasWarnings = fxMap.isNotEmpty()
        errorsCount = FXErrorMode.values().count { fxErrorMode -> fxMap.values.any { fxErrorMode.isActive(it.errorModeValue) } } +
                MXErrorMode.values().count { mxErrorMode -> mxMap.values.any { mxErrorMode.isActive(it.errorModeValue) } } +
                RoverErrorMode.values().count { roverErrorMode -> roverMap.values.any { roverErrorMode.isActive(it.errorModeValue)}}
    }

    val dailyKWHoursString: String by lazy { Formatting.FORMAT.format(dailyKWHours) }

    fun isBatteryVoltageAboveSetpoint(setpointVoltage: Float): Boolean {
        return batteryVoltage >= setpointVoltage
    }

    override fun equals(other: Any?): Boolean {
        if(this === other){
            return true
        }
        if(other is SolarPacketInfo){
            return other.dateMillis == dateMillis
                && other.deviceMap.keys == deviceMap.keys
        }
        return false
    }

    override fun hashCode(): Int {
        return dateMillis.hashCode() - fxMap.keys.hashCode() + mxMap.keys.hashCode() - batteryVoltage.hashCode()
    }

    fun getRoverId(rover: RoverStatusPacket): String{
        var counter = 0 // 0->A, 1->B, etc
        for(entry in deviceMap){
            val device = entry.value
            if(device == rover){
                break
            }
            if(device is RoverStatusPacket){
                counter++
            }
        }
        return (counter + 65).toChar().toString()
    }
}
