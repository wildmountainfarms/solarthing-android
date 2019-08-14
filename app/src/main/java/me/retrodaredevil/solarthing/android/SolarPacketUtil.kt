package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packets.identification.Identifier
import me.retrodaredevil.solarthing.solar.SolarPacket
import me.retrodaredevil.solarthing.solar.outback.fx.FXStatusPacket
import me.retrodaredevil.solarthing.solar.outback.fx.OperationalMode
import me.retrodaredevil.solarthing.solar.outback.mx.ChargerMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.ChargingState
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import java.util.*

fun getOperatingModeName(fx: FXStatusPacket): String =
    when(val mode = fx.operationalMode) {
        OperationalMode.INV_ON -> "ON"
        OperationalMode.SEARCH -> "Search"
        OperationalMode.INV_OFF -> "Off"
        OperationalMode.CHARGE -> "Charge"
        OperationalMode.SILENT -> "Silent"
        OperationalMode.FLOAT -> "Float"
        OperationalMode.EQ -> "EQ"
        else -> mode.modeName
    }
fun getChargerModeName(mx: MXStatusPacket): String =
    when(mx.chargingMode!!){
        ChargerMode.SILENT -> "Off"
        ChargerMode.FLOAT -> "Float"
        ChargerMode.BULK -> "Bulk"
        ChargerMode.ABSORB -> "Absorb"
        ChargerMode.EQ -> "EQ"
    }
fun getChargingStateName(rover: RoverStatusPacket): String =
    when(rover.chargingMode!!){
        ChargingState.DEACTIVATED -> "Off"
        ChargingState.ACTIVATED -> "On"
        ChargingState.MPPT -> "MPPT"
        ChargingState.EQ -> "EQ"
        ChargingState.BOOST -> "Boost"
        ChargingState.FLOAT -> "Float"
        ChargingState.CURRENT_LIMITING -> "Curr lim"
    }
fun getModeName(packet: SolarPacket): String =
    when(packet){
        is FXStatusPacket -> getOperatingModeName(packet)
        is MXStatusPacket -> getChargerModeName(packet)
        is RoverStatusPacket -> getChargingStateName(packet)
        else -> throw IllegalArgumentException("packet: $packet is not supported")
    }
fun getOrderedIdentifiers(identifiers: Collection<Identifier>): List<Identifier> {
    return TreeSet(identifiers).toList()
}
fun <T> getOrderedValues(map: Map<Identifier, T>): List<T> {
    val r = mutableListOf<T>()
    for(identifier in getOrderedIdentifiers(map.keys)){
        r.add(map[identifier] ?: error("getOrderedIdentifiers just changes the order, it doesn't remove keys!!"))
    }
    return r
}
