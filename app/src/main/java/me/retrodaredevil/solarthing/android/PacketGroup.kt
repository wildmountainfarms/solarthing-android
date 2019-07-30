package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.instance.InstanceFragmentIndicatorPacket
import me.retrodaredevil.solarthing.packets.instance.InstancePacket
import me.retrodaredevil.solarthing.packets.instance.InstancePacketType
import me.retrodaredevil.solarthing.packets.instance.InstanceSourcePacket

data class PacketGroup(
    val packets: Collection<Packet>,
    val dateMillis: Long
)

private class ParsedPacketGroup (
    val packets: List<Packet>,
    val dateMillis: Long,
    val sourceId: String,
    val fragmentId: Int?
)


private fun sortPackets(groups: Collection<PacketGroup>): Map<String, List<PacketGroup>> {
    val map = HashMap<String, MutableList<ParsedPacketGroup>>()
    for(group in groups){
        val packets = mutableListOf<Packet>()
        var sourceId = "default" // TODO create constant in InstanceSourcePacket
        var fragmentId: Int? = null
        for(packet in group.packets){
            if (packet is InstancePacket) {
                when(packet.packetType){
                    InstancePacketType.SOURCE -> sourceId = (packet as InstanceSourcePacket).sourceId
                    InstancePacketType.FRAGMENT_INDICATOR -> fragmentId = (packet as InstanceFragmentIndicatorPacket).fragmentId
                    else -> {}
                }
            } else {
                packets.add(packet)
            }
        }
        map.getOrPut(sourceId, ::mutableListOf).add(ParsedPacketGroup(packets, group.dateMillis, sourceId, fragmentId))
    }
    for(entry in map){
        val sourceId = entry.key
        val list = entry.value

    }
    TODO("finish this method")
}
