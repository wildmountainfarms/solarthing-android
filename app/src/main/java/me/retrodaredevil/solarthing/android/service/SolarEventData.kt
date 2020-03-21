package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.packets.collection.PacketGroup

/**
 * A class that holds solar event data from "solarthing_events"
 */
class SolarEventData {
    var packetGroups = emptyList<PacketGroup>()

    var lastUpdate: Long? = null
    var lastTimeout: Long? = null
    var lastCancel: Long? = null
}
