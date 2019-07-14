package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packets.Modes
import me.retrodaredevil.solarthing.packets.collection.PacketCollection
import me.retrodaredevil.solarthing.solar.SolarPacket
import me.retrodaredevil.solarthing.solar.SolarPacketType
import me.retrodaredevil.solarthing.solar.common.BatteryVoltagePacket
import me.retrodaredevil.solarthing.solar.outback.OutbackPacket
import me.retrodaredevil.solarthing.solar.outback.fx.*
import me.retrodaredevil.solarthing.solar.outback.mx.MXErrorMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import java.text.DecimalFormat
import kotlin.math.round


/**
 * A class that deals with making a [PacketCollection] with solar data easier to retrieve values from
 */
class SolarPacketInfo(val packetCollection: PacketCollection) {
    companion object {
        val FORMAT = DecimalFormat("0.0##")
    }
    val dateMillis = packetCollection.dateMillis

    // TODO eventually we will have to stop using ints to represent devices and will have to use Identifiers

    /** A map of the port number to the FX status packet associated with that device*/
    val fxMap: Map<Int, FXStatusPacket>
    /** A map of the port number to the MX/FM status packet associated with device*/
    val mxMap: Map<Int, MXStatusPacket>

    val deviceMap: Map<Int, SolarPacket>
    val batteryMap: Map<Int, BatteryVoltagePacket>

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
    val errorsCount: Int

    init {
        fxMap = HashMap()
        mxMap = HashMap()
        deviceMap = HashMap()
        batteryMap = HashMap()
        for(packet in packetCollection.packets){
            if(packet is OutbackPacket){
                deviceMap[packet.address] = packet
                when(packet.packetType){
                    SolarPacketType.FX_STATUS -> {
                        val fx = packet as FXStatusPacket
                        fxMap[fx.address] = fx
                        batteryMap[fx.address] = fx
                    }
                    SolarPacketType.MXFM_STATUS -> {
                        val mx = packet as MXStatusPacket
                        mxMap[mx.address] = mx
                        batteryMap[mx.address] = mx
                    }
                    SolarPacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
                    SolarPacketType.RENOGY_ROVER_STATUS -> System.err.println("Not set up for renogy packets yet!")
                    null -> throw NullPointerException("packetType is null! packet: $packet")
                    else -> System.err.println("Unknown packet type: ${packet.packetType}")
                }
            }
        }
        if(fxMap.isEmpty() || mxMap.isEmpty()){
            throw IllegalArgumentException("The packet collection must have both FX and MX/FM packets!")
        }
        val first = fxMap.values.first()
        batteryVoltage = first.batteryVoltage
        batteryVoltageString = first.batteryVoltageString

        estimatedBatteryVoltage = (round(batteryMap.values.sumByDouble { it.batteryVoltage.toDouble() } / batteryMap.size * 10) / 10).toFloat()
        estimatedBatteryVoltageString = FORMAT.format(estimatedBatteryVoltage)

        acMode = Modes.getActiveMode(ACMode::class.java, fxMap.values.first().acMode)
        generatorChargingBatteries = fxMap.values.any {
            OperationalMode.CHARGE.isActive(it.operatingMode)
                    || OperationalMode.FLOAT.isActive(it.operatingMode)
                    || OperationalMode.EQ.isActive(it.operatingMode)
        }
        load = fxMap.values.sumBy { it.outputVoltage * it.inverterCurrent }
        generatorToBatteryWattage = fxMap.values.sumBy { it.inputVoltage * it.chargerCurrent }
        generatorTotalWattage = fxMap.values.sumBy { it.inputVoltage * it.buyCurrent }

        pvWattage = mxMap.values.sumBy { it.pvCurrent * it.inputVoltage }
        pvChargerWattage = mxMap.values.sumByDouble { (it.chargerCurrent + it.ampChargerCurrent) * it.batteryVoltage.toDouble() }.toFloat() // TODO check to see if ampChargerCurrent is correct
        dailyKWHours = mxMap.values.map { it.dailyKWH }.sum()

        warningsCount = WarningMode.values().count { warningMode -> fxMap.values.any { warningMode.isActive(it.warningMode) } }
        errorsCount = FXErrorMode.values().count { fxErrorMode -> fxMap.values.any { fxErrorMode.isActive(it.errorMode) } }
            + MXErrorMode.values().count { mxErrorMode -> mxMap.values.any { mxErrorMode.isActive(it.errorMode) } }
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
        return fxMap.values.any { OperationalMode.FLOAT.isActive(it.operatingMode) }
    }

    override fun equals(other: Any?): Boolean {
        if(super.equals(other)){
            return true
        }
        if(other is SolarPacketInfo){
            return other.dateMillis == dateMillis
                && other.fxMap.keys == fxMap.keys
                && other.mxMap.keys == mxMap.keys
                && other.packetCollection.dbId == packetCollection.dbId
        }
        return false
    }

    override fun hashCode(): Int {
        return dateMillis.hashCode() - fxMap.keys.hashCode() + mxMap.keys.hashCode() - batteryVoltage.hashCode() + packetCollection.dbId.hashCode()
    }
}