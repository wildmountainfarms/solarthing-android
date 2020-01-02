package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.packets.collection.PacketGroup

class SolarEventData {
    val packetGroups = mutableListOf<PacketGroup>()

    var lastUpdate: Long? = null
    var lastTimeout: Long? = null
    var lastCancel: Long? = null
}
