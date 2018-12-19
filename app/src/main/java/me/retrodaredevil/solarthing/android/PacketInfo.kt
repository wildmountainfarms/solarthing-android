package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketType
import me.retrodaredevil.solarthing.packet.StatusPacket
import me.retrodaredevil.solarthing.packet.fx.FXStatusPacket
import me.retrodaredevil.solarthing.packet.fx.OperationalMode
import me.retrodaredevil.solarthing.packet.mxfm.MXFMStatusPacket

class PacketInfo(packetCollection: PacketCollection) {
    val dateMillis = packetCollection.dateMillis
    val fxMap: Map<Int, FXStatusPacket>
    val mxMap: Map<Int, MXFMStatusPacket>

    val batteryVoltage: Float
    val batteryVoltageString: String

    val pvWattage: Int
    val load: Int
    val generatorToBatteryWattage: Int
    val generatorTotalWattage: Int

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
        for(fx in fxMap.values){
            load += fx.outputVoltage * fx.inverterCurrent
            if(OperationalMode.FLOAT.isActive(fx.operatingMode)){ // when float is active, generator is on
                generatorOn = true
            }
            generatorToBatteryWattage += fx.inputVoltage * fx.chargerCurrent
            generatorTotalWattage += fx.inputVoltage * fx.buyCurrent
        }
        this.load = load
        this.generatorToBatteryWattage = generatorToBatteryWattage
        this.generatorTotalWattage = generatorTotalWattage
        this.generatorOn = generatorOn


        var pvWattage = 0
        var dailyKWH = 0.0F
        for(mx in mxMap.values){
            pvWattage += mx.pvCurrent * mx.inputVoltage
            dailyKWH += mx.dailyKWH

            //power to battery = mx.chargerCurrent * mx.batteryVoltage // I don't remember if this is correct or not

        }
        this.pvWattage = pvWattage
        this.dailyKWHours = dailyKWH
    }

    val pvWattageString by lazy { pvWattage.toString() }
    val dailyKWHoursString by lazy { dailyKWHours.toString() }
    val loadString by lazy { load.toString() }
    val generatorToBatteryWattageString by lazy { generatorToBatteryWattage.toString() }
    val generatorTotalWattageString by lazy { generatorTotalWattage.toString() }
}