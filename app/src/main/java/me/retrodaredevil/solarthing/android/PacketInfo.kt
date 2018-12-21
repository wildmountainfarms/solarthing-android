package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketType
import me.retrodaredevil.solarthing.packet.StatusPacket
import me.retrodaredevil.solarthing.packet.fx.FXErrorMode
import me.retrodaredevil.solarthing.packet.fx.FXStatusPacket
import me.retrodaredevil.solarthing.packet.fx.OperationalMode
import me.retrodaredevil.solarthing.packet.fx.WarningMode
import me.retrodaredevil.solarthing.packet.mxfm.MXFMErrorMode
import me.retrodaredevil.solarthing.packet.mxfm.MXFMStatusPacket

class PacketInfo(packetCollection: PacketCollection) {
    val dateMillis = packetCollection.dateMillis
    /** A map of the port number to the FX status packet associated with that device*/
    val fxMap: Map<Int, FXStatusPacket>
    /** A map of the port number to the MX/FM status packet associated with device*/
    val mxMap: Map<Int, MXFMStatusPacket>

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
            if(packet is StatusPacket){
                when(packet.packetType){
                    PacketType.FX_STATUS -> {
                        val fx = packet as FXStatusPacket
                        fxMap[fx.address] = fx
                    }
                    PacketType.MXFM_STATUS -> {
                        val mx = packet as MXFMStatusPacket
                        mxMap[mx.address] = mx
                    }
                    PacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
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
            + MXFMErrorMode.values().count { mxfmErrorMode -> mxMap.values.any { mxfmErrorMode.isActive(it.errorMode) } }
    }

    val pvWattageString by lazy { pvWattage.toString() }
    val dailyKWHoursString by lazy { dailyKWHours.toString() }
    val loadString by lazy { load.toString() }
    val generatorToBatteryWattageString by lazy { generatorToBatteryWattage.toString() }
    val generatorTotalWattageString by lazy { generatorTotalWattage.toString() }
    val fxChargerCurrentString by lazy { fxChargerCurrent.toString() }
    val fxBuyCurrentString by lazy { fxBuyCurrent.toString() }
}