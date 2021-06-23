package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.android.prefs.BatteryVoltageType
import me.retrodaredevil.solarthing.android.util.Formatting
import me.retrodaredevil.solarthing.misc.device.CpuTemperaturePacket
import me.retrodaredevil.solarthing.misc.device.DevicePacket
import me.retrodaredevil.solarthing.misc.device.DevicePacketType
import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.packets.identification.KnownIdentifierFragment
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.solar.SolarStatusPacketType
import me.retrodaredevil.solarthing.solar.batteryvoltage.BatteryVoltageOnlyPacket
import me.retrodaredevil.solarthing.solar.common.BasicChargeController
import me.retrodaredevil.solarthing.solar.common.BatteryVoltage
import me.retrodaredevil.solarthing.solar.common.DailyChargeController
import me.retrodaredevil.solarthing.solar.extra.SolarExtraPacket
import me.retrodaredevil.solarthing.solar.extra.SolarExtraPacketType
import me.retrodaredevil.solarthing.solar.outback.OutbackData
import me.retrodaredevil.solarthing.solar.outback.OutbackIdentifier
import me.retrodaredevil.solarthing.solar.outback.OutbackUtil
import me.retrodaredevil.solarthing.solar.outback.fx.*
import me.retrodaredevil.solarthing.solar.outback.fx.extra.DailyFXPacket
import me.retrodaredevil.solarthing.solar.outback.mx.MXErrorMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverErrorMode
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverIdentifier
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import me.retrodaredevil.solarthing.solar.tracer.TracerIdentifier
import me.retrodaredevil.solarthing.solar.tracer.TracerStatusPacket
import kotlin.math.round

/**
 * A class that deals with making a [PacketGroup] with solar data easier to retrieve values from
 */
class SolarPacketInfo
@Throws(CreationException::class)
constructor(
        val packetGroup: FragmentedPacketGroup,
        batteryVoltageType: BatteryVoltageType
) {
    val dateMillis = packetGroup.dateMillis


    /** A map of the port number to the FX status packet associated with that device*/
    val fxMap: Map<KnownIdentifierFragment<OutbackIdentifier>, FXStatusPacket>
    /** A map of the port number to the MX/FM status packet associated with device*/
    val mxMap: Map<KnownIdentifierFragment<OutbackIdentifier>, MXStatusPacket>
    val roverMap: Map<KnownIdentifierFragment<RoverIdentifier>, RoverStatusPacket>
    val tracerMap: Map<KnownIdentifierFragment<TracerIdentifier>, TracerStatusPacket>

    val deviceMap: Map<IdentifierFragment, SolarStatusPacket>
    val basicChargeControllerMap: Map<IdentifierFragment, BasicChargeController>
    private val rawDailyChargeControllerMap: Map<IdentifierFragment, DailyChargeController> // unused so far
    val batteryMap: Map<IdentifierFragment, BatteryVoltage>

    val dailyFXMap: Map<KnownIdentifierFragment<OutbackIdentifier>, DailyFXPacket>

    val masterFXStatusPacket: FXStatusPacket?

    /** The battery voltage */
    val batteryVoltage: Float
    /** The battery voltage as a string (in base 10)*/
    val batteryVoltageString: String

    private val estimatedBatteryVoltage: Float
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
    val acModeNullable: ACMode?
    val generatorChargingBatteries: Boolean

    val warningsCount: Int
    val hasWarnings: Boolean
    val errorsCount: Int

    val deviceCpuTemperatureMap: Map<Int, Float>

    val batteryTemperatureCelsius: Int?

    init {
        if (packetGroup.packets.isEmpty()) {
            throw CreationException("The PacketGroup cannot be empty!")
        }
        fxMap = LinkedHashMap()
        mxMap = LinkedHashMap()
        roverMap = LinkedHashMap()
        tracerMap = LinkedHashMap()
        deviceMap = LinkedHashMap()
        basicChargeControllerMap = LinkedHashMap()
        rawDailyChargeControllerMap = LinkedHashMap()
        batteryMap = LinkedHashMap()
        dailyFXMap = LinkedHashMap()
        deviceCpuTemperatureMap = LinkedHashMap()
        for(packet in packetGroup.packets){
            if(packet is SolarStatusPacket){
                val identifierFragment = IdentifierFragment.create(packetGroup.getFragmentId(packet), packet.identifier)
                deviceMap[identifierFragment] = packet
                when(packet.packetType){
                    SolarStatusPacketType.FX_STATUS -> {
                        val fx = packet as FXStatusPacket
                        fxMap[IdentifierFragment.create(packetGroup.getFragmentId(packet), packet.identifier)] = fx
                        batteryMap[identifierFragment] = fx
                    }
                    SolarStatusPacketType.MXFM_STATUS -> {
                        val mx = packet as MXStatusPacket
                        mxMap[IdentifierFragment.create(packetGroup.getFragmentId(packet), packet.identifier)] = mx
                        batteryMap[identifierFragment] = mx
                        basicChargeControllerMap[identifierFragment] = mx
                        rawDailyChargeControllerMap[identifierFragment] = mx
                    }
                    SolarStatusPacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
                    SolarStatusPacketType.RENOGY_ROVER_STATUS -> {
                        val rover = packet as RoverStatusPacket
                        roverMap[IdentifierFragment.create(packetGroup.getFragmentId(packet), packet.identifier)] = rover
                        batteryMap[identifierFragment] = rover
                        basicChargeControllerMap[identifierFragment] = rover
                        rawDailyChargeControllerMap[identifierFragment] = rover
                    }
                    SolarStatusPacketType.TRACER_STATUS -> {
                        val tracer = packet as TracerStatusPacket
                        tracerMap[IdentifierFragment.create(packetGroup.getFragmentId(packet), packet.identifier)] = tracer
                        batteryMap[identifierFragment] = tracer
                        basicChargeControllerMap[identifierFragment] = tracer
                        rawDailyChargeControllerMap[identifierFragment] = tracer
                    }
                    SolarStatusPacketType.BATTERY_VOLTAGE_ONLY -> {
                        packet as BatteryVoltageOnlyPacket
                        batteryMap[IdentifierFragment.create(packetGroup.getFragmentId(packet), packet.identifier)] = packet
                    }
                    else -> System.err.println("Unknown packet type: ${packet.packetType}")
                }
            } else if(packet is SolarExtraPacket){
                val fragmentId = packetGroup.getFragmentId(packet)
                @Suppress("DEPRECATION")
                when(packet.packetType){
                    SolarExtraPacketType.FX_DAILY -> {
                        packet as DailyFXPacket
                        dailyFXMap[IdentifierFragment.create(fragmentId, packet.identifier.supplementaryTo)] = packet
                    }
                    SolarExtraPacketType.MXFM_DAILY -> { }
                    SolarExtraPacketType.FX_CHARGING -> { }
                    else -> System.err.println("Unimplemented packet type: ${packet.packetType}")
                }
            } else if(packet is DevicePacket){
                when(packet.packetType){
                    DevicePacketType.DEVICE_CPU_TEMPERATURE -> {
                        packet as CpuTemperaturePacket
                        deviceCpuTemperatureMap[packetGroup.getFragmentId(packet)] = packet.cpuTemperatureCelsius
                    }
                    else -> System.err.println("Unimplemented device packet type: ${packet.packetType}")
                }
            }
        }
        masterFXStatusPacket = OutbackUtil.getMasterFX(fxMap.values) // TODO there may be a better way to indicate the master in the main solarthing codebase
        if (batteryMap.isEmpty()) {
            throw CreationException("The must be battery voltage packets!")
        }
        batteryVoltage = when(batteryVoltageType){
            BatteryVoltageType.AVERAGE -> batteryMap.values.let { it.sumByDouble { packet -> packet.batteryVoltage.toDouble() } / it.size }.toFloat()
            BatteryVoltageType.FIRST_PACKET -> null
            BatteryVoltageType.MOST_RECENT -> batteryMap.values.maxByOrNull { packetGroup.getDateMillis(it as Packet) }!!.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK -> batteryMap.values.firstOrNull { it is OutbackData }?.batteryVoltage
            BatteryVoltageType.FIRST_OUTBACK_FX -> fxMap.values.firstOrNull()?.batteryVoltage
        } ?: batteryMap.values.first().batteryVoltage
        batteryVoltageString = Formatting.TENTHS.format(batteryVoltage)

        estimatedBatteryVoltage = (round(batteryMap.values.sumByDouble { it.batteryVoltage.toDouble() } / batteryMap.size * 10) / 10).toFloat()
        estimatedBatteryVoltageString = Formatting.FORMAT.format(estimatedBatteryVoltage)

        acMode = fxMap.values.firstOrNull()?.acMode ?: ACMode.NO_AC
        acModeNullable = fxMap.values.firstOrNull()?.acMode
        generatorChargingBatteries = masterFXStatusPacket != null && masterFXStatusPacket.operationalMode in setOf(OperationalMode.CHARGE, OperationalMode.FLOAT, OperationalMode.EQ)
        load = fxMap.values.sumBy { it.inverterWattage }
        generatorToBatteryWattage = fxMap.values.sumBy { it.chargerWattage }
        generatorTotalWattage = fxMap.values.sumBy { it.buyWattage }

        pvWattage = basicChargeControllerMap.values.sumBy { it.pvWattage.toInt() }
        pvChargerWattage = basicChargeControllerMap.values.sumByDouble { it.chargingPower.toDouble() }.toFloat()

        warningsCount = WarningMode.values().count { warningMode -> fxMap.values.any { warningMode.isActive(it.warningModeValue) } }
        hasWarnings = fxMap.isNotEmpty()
        errorsCount = FXErrorMode.values().count { fxErrorMode -> fxMap.values.any { fxErrorMode.isActive(it.errorModeValue) } } +
                MXErrorMode.values().count { mxErrorMode -> mxMap.values.any { mxErrorMode.isActive(it.errorModeValue) } } +
                RoverErrorMode.values().count { roverErrorMode -> roverMap.values.any { roverErrorMode.isActive(it.errorModeValue)}}

        batteryTemperatureCelsius = roverMap.values.firstOrNull()?.batteryTemperatureCelsius
    }

    fun isBatteryVoltageAboveSetpoint(setpointVoltage: Float): Boolean {
        return batteryVoltage >= setpointVoltage
    }
    fun getBatteryTemperatureString(temperatureUnit: TemperatureUnit): String? {
        if (batteryTemperatureCelsius == null) {
            return null
        }
        return Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(batteryTemperatureCelsius.toFloat(), temperatureUnit)) + temperatureUnit.shortRepresentation
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

    fun getAlternateId(solarStatusPacket: SolarStatusPacket): String{
        var counter = 0 // 0->A, 1->B, etc
        for(entry in deviceMap){
            val device = entry.value
            if(device == solarStatusPacket){
                break
            }
            if(device is RoverStatusPacket || device is TracerStatusPacket){
                counter++
            }
        }
        return (counter + 65).toChar().toString()
    }
}
