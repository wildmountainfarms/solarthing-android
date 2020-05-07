package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
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
        OperationalMode.CHARGE -> "Chrg"
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
        ChargerMode.ABSORB -> "Absrb"
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
fun getModeName(packet: SolarStatusPacket): String =
    when(packet){
        is FXStatusPacket -> getOperatingModeName(packet)
        is MXStatusPacket -> getChargerModeName(packet)
        is RoverStatusPacket -> getChargingStateName(packet)
        else -> throw IllegalArgumentException("packet: $packet is not supported")
    }
fun getOrderedIdentifiers(identifiers: Collection<IdentifierFragment>): List<IdentifierFragment> {
    return TreeSet(identifiers).toList()
}
fun <T> getOrderedValues(map: Map<IdentifierFragment, T>): List<T> {
    val r = mutableListOf<T>()
    for(identifierFragment in getOrderedIdentifiers(
        map.keys
    )){
        r.add(map[identifierFragment] ?: error("getOrderedIdentifiers just changes the order, it doesn't remove keys!!"))
    }
    return r
}
