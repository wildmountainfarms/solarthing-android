package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.packets.collection.PacketGroup

/**
 * A class that holds solar event data from "solarthing_events"
 */
class PacketGroupData {
    private var _packetGroups: List<PacketGroup> = emptyList()
    @get:Synchronized
    @set:Synchronized
    var packetGroups: List<PacketGroup>
        get() = _packetGroups
        private set(value) { _packetGroups = value }

    var lastUpdate: Long? = null
    var lastTimeout: Long? = null
    var lastCancel: Long? = null

    @Synchronized
    fun onAllPacketReceive(packets: List<PacketGroup>){
        packetGroups = packets
        lastUpdate = System.currentTimeMillis()
    }
    @Synchronized
    fun addReceivedPackets(packets: List<PacketGroup>) {
        packetGroups = packetGroups + packets
        lastUpdate = System.currentTimeMillis()
    }

    @Synchronized
    fun getLatestPacketGroups(): Pair<List<PacketGroup>, Long?> {
        return Pair(packetGroups, lastUpdate)
    }
}
