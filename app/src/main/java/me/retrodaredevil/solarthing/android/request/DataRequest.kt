package me.retrodaredevil.solarthing.android.request

import me.retrodaredevil.solarthing.database.MillisQuery
import me.retrodaredevil.solarthing.packets.collection.StoredPacketGroup


/**
 * Represents a completed data request that may or may not have been successful
 */
data class DataRequest(
        val packetGroupList: List<StoredPacketGroup>,
        /** The query used when getting the packets. Note that if [successful] is true, this is non-null*/
        val query: MillisQuery?,
        val successful: Boolean,
        val simpleStatus: String,
        val host: String?,
        val stackTrace: String? = null,
        val errorMessage: String? = null,
)
