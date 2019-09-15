package me.retrodaredevil.solarthing.android.request

import me.retrodaredevil.solarthing.packets.collection.PacketGroup


/**
 * Represents a completed data request that may or may not have been successful
 */
data class DataRequest(
    val packetGroupList: List<PacketGroup>,
    val successful: Boolean,
    val simpleStatus: String,
    val host: String?,
    val stackTrace: String? = null,
    val errorMessage: String? = null,
    val authDebug: String? = null
)
