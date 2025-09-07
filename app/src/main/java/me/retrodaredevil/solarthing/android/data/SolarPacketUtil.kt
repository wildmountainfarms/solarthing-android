@file:Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")

package me.retrodaredevil.solarthing.android.data

import me.retrodaredevil.solarthing.packets.collection.FragmentUtil
import me.retrodaredevil.solarthing.packets.identification.Identifier
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.packets.identification.SupplementaryIdentifier
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.solar.outback.OutbackIdentifier
import me.retrodaredevil.solarthing.solar.outback.fx.FXStatusPacket
import me.retrodaredevil.solarthing.solar.outback.fx.OperationalMode
import me.retrodaredevil.solarthing.solar.outback.mx.ChargerMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.ChargingState
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverIdentifier
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import me.retrodaredevil.solarthing.solar.tracer.TracerStatusPacket
import me.retrodaredevil.solarthing.solar.tracer.mode.ChargingStatus
import java.util.*

fun getOperatingModeName(fx: FXStatusPacket): String =
    when(val mode = fx.operationalMode) {
        OperationalMode.INV_ON -> "on"
        OperationalMode.SEARCH -> "Search"
        OperationalMode.INV_OFF -> "off"
        OperationalMode.CHARGE -> "chg"
        OperationalMode.SILENT -> "silent"
        OperationalMode.FLOAT -> "flt"
        OperationalMode.EQ -> "eq"
        else -> mode.modeName
    }
fun getChargerModeName(mx: MXStatusPacket): String =
    when(mx.chargingMode){
        ChargerMode.SILENT -> "off"
        ChargerMode.FLOAT -> "flt"
        ChargerMode.BULK -> "blk"
        ChargerMode.ABSORB -> "absb"
        ChargerMode.EQ -> "eq"
    }
fun getChargingStateName(rover: RoverStatusPacket): String =
    when(rover.chargingMode){
        ChargingState.DEACTIVATED -> "off"
        ChargingState.ACTIVATED -> "on"
        ChargingState.MPPT -> "blk"
        ChargingState.EQ -> "eq"
        ChargingState.BOOST -> "boost"
        ChargingState.FLOAT -> "float"
        ChargingState.CURRENT_LIMITING -> "curr lim"
        ChargingState.DIRECT_CHARGE -> "direct"
    }
fun getChargingStatusName(tracer: TracerStatusPacket): String {
    return when(tracer.chargingStatus) {
        ChargingStatus.NO_CHARGING -> "off"
        ChargingStatus.BOOST -> "blk"
        ChargingStatus.EQUALIZATION -> "eq"
        ChargingStatus.FLOAT -> "flt"
    }
}
fun getModeName(packet: SolarStatusPacket): String =
    when(packet){
        is FXStatusPacket -> getOperatingModeName(packet)
        is MXStatusPacket -> getChargerModeName(packet)
        is RoverStatusPacket -> getChargingStateName(packet)
        is TracerStatusPacket -> getChargingStatusName(packet)
        else -> throw IllegalArgumentException("packet: $packet is not supported")
    }
private fun compareIdentifier(identifier1: Identifier, identifier2: Identifier): Int {
    if (identifier1 is SupplementaryIdentifier) {
        return compareIdentifier(identifier1.supplementaryTo, identifier2)
    }
    if (identifier2 is SupplementaryIdentifier) {
        return compareIdentifier(identifier1, identifier2.supplementaryTo)
    }
    if (identifier1 is OutbackIdentifier) {
        if (identifier2 is OutbackIdentifier) {
            return identifier1.compareTo(identifier2)
        }
        return -1
    }
    if (identifier1 is RoverIdentifier) {
        if (identifier2 is RoverIdentifier) {
            return identifier1.compareTo(identifier2) // this should always be 0
        }
        return -1
    }
    return identifier1.hashCode().compareTo(identifier2.hashCode()) // this is probably not how this should be done, but hey, we don't care
}
fun getOrderedIdentifiers(identifiers: Collection<IdentifierFragment>): List<IdentifierFragment> {
    return TreeSet<IdentifierFragment> { o1, o2 ->
        val r = FragmentUtil.DEFAULT_FRAGMENT_ID_COMPARATOR.compare(o1.fragmentId, o2.fragmentId)
        if (r != 0) r else compareIdentifier(o1.identifier, o2.identifier)
    }.apply { addAll(identifiers) }.toList()
}
fun <T> getOrderedValues(map: Map<out IdentifierFragment, T>): List<T> {
    val r = mutableListOf<T>()
    for (identifierFragment in getOrderedIdentifiers(map.keys)) {
        r.add(map[identifierFragment] ?: error("getOrderedIdentifiers just changes the order, it doesn't remove keys!!"))
    }
    return r
}
