package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.android.prefs.BatteryVoltageType
import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.packets.identification.Identifier
import me.retrodaredevil.solarthing.solar.SolarPacket
import me.retrodaredevil.solarthing.solar.SolarPacketType
import me.retrodaredevil.solarthing.solar.common.BatteryVoltage
import me.retrodaredevil.solarthing.solar.common.ChargeController
import me.retrodaredevil.solarthing.solar.common.DailyData
import me.retrodaredevil.solarthing.solar.outback.OutbackPacket
import me.retrodaredevil.solarthing.solar.outback.fx.*
import me.retrodaredevil.solarthing.solar.outback.mx.MXErrorMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverErrorMode
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverIdentifier
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import java.text.DecimalFormat
import kotlin.math.round


/**
 * A class that deals with making a [PacketGroup] with solar data easier to retrieve values from
 */
class SolarPacketInfo(
    val packetGroup: PacketGroup,
    val batteryVoltageType: BatteryVoltageType
) {
    companion object {
        val TENTHS_FORMAT = DecimalFormat("0.0")
        val FORMAT = DecimalFormat("0.0##")
    }
    val dateMillis = packetGroup.dateMillis


    /** A map of the port number to the FX status packet associated with that device*/
    val fxMap: Map<Identifier, FXStatusPacket>
    /** A map of the port number to the MX/FM status packet associated with device*/
    val mxMap: Map<Identifier, MXStatusPacket>
    val roverMap: Map<RoverIdentifier, RoverStatusPacket>

    val deviceMap: Map<Identifier, SolarPacket>
    val chargeControllerMap: Map<Identifier, ChargeController>
    val dailyDataMap: Map<Identifier, DailyData>
    val batteryMap: Map<Identifier, BatteryVoltage>

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

    /**
     * The AC Mode which can be used to determine the state of the generator
     */
    val acMode: ACMode
    val generatorChargingBatteries: Boolean

    val dailyKWHours: Float

    val warningsCount: Int
    val hasWarnings: Boolean
    val errorsCount: Int

    init {
        fxMap = LinkedHashMap()
        mxMap = LinkedHashMap()
        roverMap = LinkedHashMap()
        deviceMap = LinkedHashMap()
        chargeControllerMap = LinkedHashMap()
        dailyDataMap = LinkedHashMap()
        batteryMap = LinkedHashMap()
        for(packet in packetGroup.packets){
            if(packet is SolarPacket){
                deviceMap[packet.identifier] = packet
                when(packet.packetType){
                    SolarPacketType.FX_STATUS -> {
                        val fx = packet as FXStatusPacket
                        fxMap[fx.identifier] = fx
                        batteryMap[fx.identifier] = fx
                    }
                    SolarPacketType.MXFM_STATUS -> {
                        val mx = packet as MXStatusPacket
                        mxMap[mx.identifier] = mx
                        batteryMap[mx.identifier] = mx
                        chargeControllerMap[mx.identifier] = mx
                        dailyDataMap[mx.identifier] = mx
                    }
                    SolarPacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
                    SolarPacketType.RENOGY_ROVER_STATUS -> {
                        val rover = packet as RoverStatusPacket
                        roverMap[rover.identifier] = rover
                        batteryMap[rover.identifier] = rover
                        chargeControllerMap[rover.identifier] = rover
                        dailyDataMap[rover.identifier] = rover
                    }
                    null -> throw NullPointerException("packetType is null! packet: $packet")
                    else -> System.err.println("Unknown packet type: ${packet.packetType}")
                }
            }
        }
        batteryVoltage = when(batteryVoltageType){
            BatteryVoltageType.AVERAGE -> batteryMap.values.let { it.sumByDouble { packet -> packet.batteryVoltage.toDouble() } / it.size }.toFloat()
            BatteryVoltageType.FIRST_PACKET -> batteryMap.values.first().batteryVoltage
            BatteryVoltageType.MOST_RECENT -> batteryMap.values.maxBy { packetGroup.getDateMillis(it as Packet) }!!.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK -> batteryMap.values.first { it is OutbackPacket }.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK_FX -> fxMap.values.first().batteryVoltage
        }
        batteryVoltageString = TENTHS_FORMAT.format(batteryVoltage)

        estimatedBatteryVoltage = (round(batteryMap.values.sumByDouble { it.batteryVoltage.toDouble() } / batteryMap.size * 10) / 10).toFloat()
        estimatedBatteryVoltageString = FORMAT.format(estimatedBatteryVoltage)

        acMode = if(fxMap.isNotEmpty()) fxMap.values.first().acModeMode else ACMode.NO_AC
        generatorChargingBatteries = if(fxMap.isEmpty()) false else fxMap.values.any {
            OperationalMode.CHARGE.isActive(it.operatingModeValue)
                    || OperationalMode.FLOAT.isActive(it.operatingModeValue)
                    || OperationalMode.EQ.isActive(it.operatingModeValue)
        }
        load = fxMap.values.sumBy { it.outputVoltage * it.inverterCurrent }
        generatorToBatteryWattage = fxMap.values.sumBy { it.inputVoltage * it.chargerCurrent }
        generatorTotalWattage = fxMap.values.sumBy { it.inputVoltage * it.buyCurrent }

        pvWattage = chargeControllerMap.values.sumByDouble { it.pvCurrent.toDouble() * it.inputVoltage.toDouble() }.toInt()
        pvChargerWattage = chargeControllerMap.values.sumByDouble { it.chargingPower.toDouble() }.toFloat()
        dailyKWHours = dailyDataMap.values.map { it.dailyKWH }.sum()

        warningsCount = WarningMode.values().count { warningMode -> fxMap.values.any { warningMode.isActive(it.warningMode) } }
        hasWarnings = fxMap.isNotEmpty()
        errorsCount = FXErrorMode.values().count { fxErrorMode -> fxMap.values.any { fxErrorMode.isActive(it.errorMode) } }
            + MXErrorMode.values().count { mxErrorMode -> mxMap.values.any { mxErrorMode.isActive(it.errorMode) } }
            + RoverErrorMode.values().count { roverErrorMode -> roverMap.values.any { roverErrorMode.isActive(it.errorMode)}}
    }

    val pvWattageString by lazy { pvWattage.toString() }
    val pvChargerWattageString: String by lazy { FORMAT.format(pvChargerWattage) }
    val dailyKWHoursString: String by lazy { FORMAT.format(dailyKWHours) }
    val loadString by lazy { load.toString() }
    val generatorToBatteryWattageString by lazy { generatorToBatteryWattage.toString() }
    val generatorTotalWattageString by lazy { generatorTotalWattage.toString() }

    /**
     * Because older firmware doesn't always report the FXs being in float mode, we can use a custom battery voltage
     * check to see if they should be in float mode.
     * @param virtualFloatModeMinimumBatteryVoltage The minimum battery voltage this needs for this method to return true
     *                                              or null to only check the FXs for being in float mode
     * @return true if any of the FXs are in float mode or if the batteryVoltage >= virtualFloatModeMinimumBatteryVoltage
     */
    fun isGeneratorInFloat(virtualFloatModeMinimumBatteryVoltage: Float?): Boolean {
        if(virtualFloatModeMinimumBatteryVoltage != null && batteryVoltage >= virtualFloatModeMinimumBatteryVoltage && acMode == ACMode.AC_USE){
            return true
        }
        return fxMap.values.any { OperationalMode.FLOAT.isActive(it.operatingModeValue) }
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

    fun getRoverID(rover: RoverStatusPacket): String{
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