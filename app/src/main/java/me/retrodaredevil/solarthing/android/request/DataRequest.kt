package me.retrodaredevil.solarthing.android.request

import me.retrodaredevil.solarthing.packet.PacketCollection
import org.lightcouch.CouchDbProperties

/**
 * Represents a completed data request that may or may not have been successful
 */
data class DataRequest(
    val packetCollectionList: List<PacketCollection>,
    val successful: Boolean,
    val simpleStatus: String,
    val stackTrace: String? = null,
    val errorMessage: String? = null,
    val couchDbProperties: CouchDbProperties? = null
)
