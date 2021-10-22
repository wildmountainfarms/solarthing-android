package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.database.VersionedPacket
import me.retrodaredevil.solarthing.type.alter.StoredAlterPacket

class AlterHandler {
    @get:Synchronized
    var alterPackets: List<VersionedPacket<StoredAlterPacket>>? = null
        private set

    @get:Synchronized
    var lastSuccess: Long? = null
        private set

    @get:Synchronized
    var lastFail: Long? = null
        private set

    @Synchronized
    fun update(packets: List<VersionedPacket<StoredAlterPacket>>) {
        alterPackets = packets
        lastSuccess = System.currentTimeMillis()
    }
    @Synchronized
    fun fail() {
        lastFail = System.currentTimeMillis()
    }
}