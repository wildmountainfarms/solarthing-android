package me.retrodaredevil.solarthing.android

import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.packets.Packet

sealed class PacketParse {
    class Success(val packetGroup: PacketGroup) : PacketParse()
    class Failure(val exception: Exception) : PacketParse()
}

fun parsePacketGroup(packetGroup: JsonObject, jsonPacketGetter: (JsonObject) -> Packet): PacketParse{
    val dateMillis = packetGroup.getAsJsonPrimitive("dateMillis").asLong
    val jsonPackets = packetGroup.getAsJsonArray("packets")
    val packets = mutableListOf<Packet>()
    var exception: Exception? = null
    for(jsonPacket in jsonPackets) {
        val packet: Packet
        try {
            //PacketCollections.createFromJson(jsonObject.getAsJsonObject("value"), jsonPacketGetter)
            packet = jsonPacketGetter(jsonPacket.asJsonObject)
        } catch (ex: Exception) {
            exception = ex
            ex.printStackTrace()
            continue
        }
        packets.add(packet)
    }
    if(packets.isEmpty() && exception != null){
        return PacketParse.Failure(exception)
    }
    return PacketParse.Success(PacketGroup(packets, dateMillis))
}
