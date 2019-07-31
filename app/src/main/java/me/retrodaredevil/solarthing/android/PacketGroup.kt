package me.retrodaredevil.solarthing.android

import android.annotation.SuppressLint
import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.instance.InstanceFragmentIndicatorPacket
import me.retrodaredevil.solarthing.packets.instance.InstancePacket
import me.retrodaredevil.solarthing.packets.instance.InstancePacketType
import me.retrodaredevil.solarthing.packets.instance.InstanceSourcePacket
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.absoluteValue

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


fun sortPackets(groups: Collection<PacketGroup>, maxTimeDistance: Long = (60 * 1000)): Map<String, List<PacketGroup>> {
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
    val r = HashMap<String, List<PacketGroup>>()
    for(entry in map){
        val sourceId = entry.key // sourceId will be the same for everything in list
        val list = entry.value

        @SuppressLint("UseSparseArrays") // TODO we could actually make this a SparseArray later
        val fragmentMap = HashMap<Int?, MutableList<ParsedPacketGroup>>()
        for(packetGroup in list){
            fragmentMap.getOrPut(packetGroup.fragmentId, ::mutableListOf).add(packetGroup)
        }
        val fragmentIds = TreeSet<Int?>{ o1, o2 ->
            when { // null is last in the set. Other values are ascending
                o1 == null -> 1
                o2 == null -> -1
                else -> o1 - o2
            }
        }.apply {
            addAll(fragmentMap.keys)
        }.toList()
        val masterFragmentId = fragmentIds[0]
        val masterList = fragmentMap[masterFragmentId]!!
        val packetGroups = mutableListOf<PacketGroup>()
        for(masterGroup in masterList){
            val packetList = mutableListOf<Packet>()
            packetList.addAll(masterGroup.packets)
            for(fragmentId in fragmentIds){
                if(fragmentId == masterFragmentId) continue
                val packetGroupList: List<ParsedPacketGroup> = fragmentMap[fragmentId]!!
                // now we want to find the closest packet group
                // TODO This is a perfect place to use binary search
                var closest: ParsedPacketGroup? = null
                var smallestTime: Long? = null
                for(packetGroup in packetGroupList){
                    val timeDistance = (packetGroup.dateMillis - masterGroup.dateMillis).absoluteValue
                    if(smallestTime == null || timeDistance < smallestTime){
                        closest = packetGroup
                        smallestTime = timeDistance
                    }
                }
                closest!!
                smallestTime!!
                if(smallestTime < maxTimeDistance){
                    packetList.addAll(closest.packets)
                }
            }
            packetGroups.add(PacketGroup(packetList, masterGroup.dateMillis)) // TODO it may be useful to provide more dateMillis info on other fragments
        }
        r[sourceId] = packetGroups
    }
    return r
}
