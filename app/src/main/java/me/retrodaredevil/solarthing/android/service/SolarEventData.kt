package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.packets.collection.PacketGroup

/**
 * A class that holds solar event data from "solarthing_events"
 */
class SolarEventData {
    private var _packetsGroups: List<PacketGroup> = emptyList()
    @get:Synchronized
    @set:Synchronized
    var packetGroups: List<PacketGroup>
        get() = _packetsGroups
        private set(value) { _packetsGroups = value }

    var lastUpdate: Long? = null
    var lastTimeout: Long? = null
    var lastCancel: Long? = null

    @Synchronized
    fun onPacketReceive(packets: List<PacketGroup>){
        packetGroups = packets
        lastUpdate = System.currentTimeMillis()
    }

    @Synchronized
    fun getLatestPacketGroups(): Pair<List<PacketGroup>, Long?> {
        return Pair(packetGroups, lastUpdate)
    }
}
