package me.retrodaredevil.solarthing.android.request

import me.retrodaredevil.solarthing.packet.PacketCollection

/**
 * Represents a completed data request that may or may not have been successful
 */
data class DataRequest(
    val packetCollectionList: List<PacketCollection>,
    val successful: Boolean,
    val simpleStatus: String,
    val stackTrace: String?
)
