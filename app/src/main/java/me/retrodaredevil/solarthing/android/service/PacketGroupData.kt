package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.database.MillisQuery
import me.retrodaredevil.solarthing.database.cache.DatabaseCache
import me.retrodaredevil.solarthing.database.cache.SimpleDatabaseCache
import me.retrodaredevil.solarthing.packets.collection.StoredPacketGroup
import me.retrodaredevil.solarthing.util.TimeRange
import java.time.Duration
import java.time.Instant

class PacketGroupData {
    /** The cache. Note this is not thread safe, so this class synchronizes when using it */
    private val cache = SimpleDatabaseCache.createDefault()

    fun <T> useCache(function: (DatabaseCache) -> T): T {
        synchronized(this) {
            return function(cache)
        }
    }
    fun <T> useCacheGetLastUpdate(function: (DatabaseCache) -> T): Pair<T, Long?> {
        synchronized(this) {
            return Pair(function(cache), lastUpdate)
        }
    }

    var lastUpdate: Long? = null
    var lastTimeout: Long? = null
    var lastCancel: Long? = null

    @Synchronized
    fun feed(packetGroups: List<StoredPacketGroup>, query: MillisQuery) {
        cache.feed(packetGroups, query.startKey!!, query.endKey)
        lastUpdate = System.currentTimeMillis()
    }

    fun getLatestPacket(): StoredPacketGroup? {
        return useCache { it.createAllCachedPacketsStream(true).findFirst().orElse(null) }
    }
    val recommendedQuery: MillisQuery
        get() = cache.recommendedQuery

    fun getLastPacketsGroups(last: Duration): List<StoredPacketGroup> {
        return useCache { it.getCachedPacketsInRange(TimeRange.createAfter(Instant.now() - last), false) }
    }

}
