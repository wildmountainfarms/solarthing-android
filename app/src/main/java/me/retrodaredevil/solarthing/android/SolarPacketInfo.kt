package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packets.collection.PacketCollection
import me.retrodaredevil.solarthing.solar.SolarPacket
import me.retrodaredevil.solarthing.solar.SolarPacketType
import me.retrodaredevil.solarthing.solar.fx.FXErrorMode
import me.retrodaredevil.solarthing.solar.fx.FXStatusPacket
import me.retrodaredevil.solarthing.solar.fx.OperationalMode
import me.retrodaredevil.solarthing.solar.fx.WarningMode
import me.retrodaredevil.solarthing.solar.mx.MXErrorMode
import me.retrodaredevil.solarthing.solar.mx.MXStatusPacket


/**
 * A class that deals with making a [PacketCollection] with solar data easier to retrieve values from
 */
class SolarPacketInfo(private val packetCollection: PacketCollection) {
    val dateMillis = packetCollection.dateMillis
    /** A map of the port number to the FX status packet associated with that device*/
    val fxMap: Map<Int, FXStatusPacket>
    /** A map of the port number to the MX/FM status packet associated with device*/
    val mxMap: Map<Int, MXStatusPacket>

    /** The battery voltage */
    val batteryVoltage: Float
    /** The battery voltage as a string (in base 10)*/
    val batteryVoltageString: String

    /** The PV Wattage (Power from the solar panels)*/
    val pvWattage: Int
    /** The load from the FX's */
    val load: Int
    /** The power from the generator going into the batteries*/
    val generatorToBatteryWattage: Int
    /** The total power from the generator */
    val generatorTotalWattage: Int

    val fxChargerCurrent: Int
    val fxBuyCurrent: Int

    /** true if the generator is on, false otherwise*/
    val generatorOn: Boolean

    val dailyKWHours: Float

    val warningsCount: Int
    val errorsCount: Int

    init {
        fxMap = HashMap()
        mxMap = HashMap()
        for(packet in packetCollection.packets){
            if(packet is SolarPacket){
                when(packet.packetType){
                    SolarPacketType.FX_STATUS -> {
                        val fx = packet as FXStatusPacket
                        fxMap[fx.address] = fx
                    }
                    SolarPacketType.MXFM_STATUS -> {
                        val mx = packet as MXStatusPacket
                        mxMap[mx.address] = mx
                    }
                    SolarPacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
                    null -> throw NullPointerException("packetType is null! packet: $packet")
                }
            }
        }
        if(fxMap.isEmpty() || mxMap.isEmpty()){
            throw IllegalArgumentException("The packet collection must have both FX and MX/FM packets!")
        }
        val first = fxMap.values.first()
        this.batteryVoltage = first.batteryVoltage
        this.batteryVoltageString = first.batteryVoltageString

        generatorOn = fxMap.values.any { OperationalMode.CHARGE.isActive(it.operatingMode) || OperationalMode.FLOAT.isActive(it.operatingMode) }
        load = fxMap.values.sumBy { it.outputVoltage * it.inverterCurrent }
        generatorToBatteryWattage = fxMap.values.sumBy { it.inputVoltage * it.chargerCurrent }
        generatorTotalWattage = fxMap.values.sumBy { it.inputVoltage * it.buyCurrent }
        fxChargerCurrent = fxMap.values.sumBy { it.chargerCurrent }
        fxBuyCurrent = fxMap.values.sumBy { it.buyCurrent }

        pvWattage = mxMap.values.sumBy { it.pvCurrent * it.inputVoltage }
        dailyKWHours = mxMap.values.map { it.dailyKWH }.sum()

        warningsCount = WarningMode.values().count { warningMode -> fxMap.values.any { warningMode.isActive(it.warningMode) } }
        errorsCount = FXErrorMode.values().count { fxErrorMode -> fxMap.values.any { fxErrorMode.isActive(it.errorMode) } }
            + MXErrorMode.values().count { mxErrorMode -> mxMap.values.any { mxErrorMode.isActive(it.errorMode) } }
    }

    val pvWattageString by lazy { pvWattage.toString() }
    val dailyKWHoursString by lazy { dailyKWHours.toString() }
    val loadString by lazy { load.toString() }
    val generatorToBatteryWattageString by lazy { generatorToBatteryWattage.toString() }
    val generatorTotalWattageString by lazy { generatorTotalWattage.toString() }
    val fxChargerCurrentString by lazy { fxChargerCurrent.toString() }
    val fxBuyCurrentString by lazy { fxBuyCurrent.toString() }

    /**
     * Because older firmware doesn't always report the FXs being in float mode, we can use a custom battery voltage
     * check to see if they should be in float mode.
     * @param virtualFloatModeMinimumBatteryVoltage The minimum battery voltage this needs for this method to return true
     *                                              or null to only check the FXs for being in float mode
     * @return true if any of the FXs are in float mode or if the batteryVoltage >= virtualFloatModeMinimumBatteryVoltage
     */
    fun isGeneratorInFloat(virtualFloatModeMinimumBatteryVoltage: Float?): Boolean {
        if(virtualFloatModeMinimumBatteryVoltage != null && batteryVoltage >= virtualFloatModeMinimumBatteryVoltage && generatorOn){
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