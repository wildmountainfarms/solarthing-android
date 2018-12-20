package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketType
import me.retrodaredevil.solarthing.packet.StatusPacket
import me.retrodaredevil.solarthing.packet.fx.FXStatusPacket
import me.retrodaredevil.solarthing.packet.fx.OperationalMode
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

        var load = 0
        var generatorOn = false
        var generatorToBatteryWattage = 0
        var generatorTotalWattage = 0
        var fxBuyCurrent = 0
        var fxChargerCurrent = 0
        for(fx in fxMap.values){
            load += fx.outputVoltage * fx.inverterCurrent
            if((fx.buyCurrent > 0 && OperationalMode.CHARGE.isActive(fx.operatingMode))
                || OperationalMode.FLOAT.isActive(fx.operatingMode)){
                generatorOn = true
            }
            fxChargerCurrent += fx.chargerCurrent
            fxBuyCurrent += fx.buyCurrent

            generatorToBatteryWattage += fx.inputVoltage * fx.chargerCurrent
            generatorTotalWattage += fx.inputVoltage * fx.buyCurrent
        }
        this.load = load

        this.fxChargerCurrent = fxChargerCurrent
        this.fxBuyCurrent = fxBuyCurrent

        this.generatorToBatteryWattage = generatorToBatteryWattage
        this.generatorTotalWattage = generatorTotalWattage

        this.generatorOn = generatorOn


        var pvWattage = 0
        var dailyKWH = 0.0F
        for(mx in mxMap.values){
            pvWattage += mx.pvCurrent * mx.inputVoltage
            dailyKWH += mx.dailyKWH

            //power to battery = mx.fxChargerCurrent * mx.batteryVoltage // I don't remember if this is correct or not

        }
        this.pvWattage = pvWattage
        this.dailyKWHours = dailyKWH
    }

    val pvWattageString by lazy { pvWattage.toString() }
    val dailyKWHoursString by lazy { dailyKWHours.toString() }
    val loadString by lazy { load.toString() }
    val generatorToBatteryWattageString by lazy { generatorToBatteryWattage.toString() }
    val generatorTotalWattageString by lazy { generatorTotalWattage.toString() }
    val fxChargerCurrentString by lazy { fxChargerCurrent.toString() }
    val fxBuyCurrentString by lazy { fxBuyCurrent.toString() }
}