package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.android.prefs.BatteryVoltageType
import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.packets.identification.Identifier
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.solar.SolarStatusPacketType
import me.retrodaredevil.solarthing.solar.common.BasicChargeController
import me.retrodaredevil.solarthing.solar.common.BatteryVoltage
import me.retrodaredevil.solarthing.solar.common.DailyChargeController
import me.retrodaredevil.solarthing.solar.extra.SolarExtraPacket
import me.retrodaredevil.solarthing.solar.extra.SolarExtraPacketType
import me.retrodaredevil.solarthing.solar.outback.OutbackData
import me.retrodaredevil.solarthing.solar.outback.fx.*
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
    val packetGroup: PacketGroup,
    batteryVoltageType: BatteryVoltageType
) {
    val dateMillis = packetGroup.dateMillis


    /** A map of the port number to the FX status packet associated with that device*/
    val fxMap: Map<Identifier, FXStatusPacket>
    /** A map of the port number to the MX/FM status packet associated with device*/
    val mxMap: Map<Identifier, MXStatusPacket>
    val roverMap: Map<RoverIdentifier, RoverStatusPacket>

    val deviceMap: Map<Identifier, SolarStatusPacket>
    val basicChargeControllerMap: Map<Identifier, BasicChargeController>
    val rawDailyChargeControllerMap: Map<Identifier, DailyChargeController>
    val dailyChargeControllerMap: Map<Identifier, DailyChargeController>
    val batteryMap: Map<Identifier, BatteryVoltage>

    val dailyFXMap: Map<Identifier, DailyFXPacket>
    val dailyMXMap: Map<Identifier, DailyMXPacket>

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
        for(packet in packetGroup.packets){
            if(packet is SolarStatusPacket){
                deviceMap[packet.identifier] = packet
                when(packet.packetType){
                    SolarStatusPacketType.FX_STATUS -> {
                        val fx = packet as FXStatusPacket
                        fxMap[fx.identifier] = fx
                        batteryMap[fx.identifier] = fx
                    }
                    SolarStatusPacketType.MXFM_STATUS -> {
                        val mx = packet as MXStatusPacket
                        mxMap[mx.identifier] = mx
                        batteryMap[mx.identifier] = mx
                        basicChargeControllerMap[mx.identifier] = mx
                        rawDailyChargeControllerMap[mx.identifier] = mx
                        (dailyChargeControllerMap as MutableMap).getOrPut(mx.identifier) { mx }
                    }
                    SolarStatusPacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
                    SolarStatusPacketType.RENOGY_ROVER_STATUS -> {
                        val rover = packet as RoverStatusPacket
                        roverMap[rover.identifier] = rover
                        batteryMap[rover.identifier] = rover
                        basicChargeControllerMap[rover.identifier] = rover
                        rawDailyChargeControllerMap[rover.identifier] = rover
                        dailyChargeControllerMap[rover.identifier] = rover
                    }
                    null -> throw NullPointerException("packetType is null! packet: $packet")
                    else -> System.err.println("Unknown packet type: ${packet.packetType}")
                }
            } else if(packet is SolarExtraPacket){
                when(packet.packetType){
                    SolarExtraPacketType.FX_DAILY -> {
                        packet as DailyFXPacket
                        dailyFXMap[packet.identifier.supplementaryTo] = packet
                    }
                    SolarExtraPacketType.MXFM_DAILY -> {
                        packet as DailyMXPacket
                        dailyMXMap[packet.identifier.supplementaryTo] = packet
                        dailyChargeControllerMap[packet.identifier.supplementaryTo] = packet
                    }
                    null -> throw NullPointerException("packetType is null! packet: $packet")
                    else -> System.err.println("Unimplemented packet type: ${packet.packetType}")
                }
            }
        }
        batteryVoltage = when(batteryVoltageType){
            BatteryVoltageType.AVERAGE -> batteryMap.values.let { it.sumByDouble { packet -> packet.batteryVoltage.toDouble() } / it.size }.toFloat()
            BatteryVoltageType.FIRST_PACKET -> batteryMap.values.first().batteryVoltage
            BatteryVoltageType.MOST_RECENT -> batteryMap.values.maxBy { packetGroup.getDateMillis(it as Packet) }!!.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK -> batteryMap.values.first { it is OutbackData }.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK_FX -> fxMap.values.first().batteryVoltage
        }
        batteryVoltageString = Formatting.TENTHS.format(batteryVoltage)

        estimatedBatteryVoltage = (round(batteryMap.values.sumByDouble { it.batteryVoltage.toDouble() } / batteryMap.size * 10) / 10).toFloat()
        estimatedBatteryVoltageString = Formatting.FORMAT.format(estimatedBatteryVoltage)

        acMode = if(fxMap.isNotEmpty()) fxMap.values.first().acMode else ACMode.NO_AC
        generatorChargingBatteries = if(fxMap.isEmpty()) false else fxMap.values.any {
            OperationalMode.CHARGE.isActive(it.operationalModeValue)
                    || OperationalMode.FLOAT.isActive(it.operationalModeValue)
                    || OperationalMode.EQ.isActive(it.operationalModeValue)
        }
        load = fxMap.values.sumBy { it.inverterWattage }
        generatorToBatteryWattage = fxMap.values.sumBy { it.chargerWattage }
        generatorTotalWattage = fxMap.values.sumBy { it.buyWattage }

        pvWattage = basicChargeControllerMap.values.sumByDouble { it.pvCurrent.toDouble() * it.inputVoltage.toDouble() }.toInt()
        pvChargerWattage = basicChargeControllerMap.values.sumByDouble { it.chargingPower.toDouble() }.toFloat()
        dailyKWHours = dailyChargeControllerMap.values.map { it.dailyKWH }.sum()

        dailyFXInfo = if(dailyFXMap.isEmpty()){
            null
        } else {
            DailyFXInfo(
                    dailyFXMap.values.sumByDouble { it.inverterKWH.toDouble() }.toFloat(),
                    dailyFXMap.values.sumByDouble { it.buyKWH.toDouble() }.toFloat(),
                    dailyFXMap.values.sumByDouble { it.chargerKWH.toDouble() }.toFloat(),
                    dailyFXMap.values.sumByDouble { it.inverterKWH.toDouble() }.toFloat()
            )
        }

        warningsCount = WarningMode.values().count { warningMode -> fxMap.values.any { warningMode.isActive(it.warningModeValue) } }
        hasWarnings = fxMap.isNotEmpty()
        errorsCount = FXErrorMode.values().count { fxErrorMode -> fxMap.values.any { fxErrorMode.isActive(it.errorModeValue) } }
                + MXErrorMode.values().count { mxErrorMode -> mxMap.values.any { mxErrorMode.isActive(it.errorModeValue) } }
                + RoverErrorMode.values().count { roverErrorMode -> roverMap.values.any { roverErrorMode.isActive(it.errorModeValue)}}
    }

    @Deprecated("")
    val pvWattageString by lazy { pvWattage.toString() }
    val dailyKWHoursString: String by lazy { Formatting.FORMAT.format(dailyKWHours) }

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
        return fxMap.values.any { OperationalMode.FLOAT.isActive(it.operationalModeValue) }
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
