package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.database.SolarThingDatabase
import me.retrodaredevil.solarthing.database.VersionedPacket
import me.retrodaredevil.solarthing.database.exception.SolarThingDatabaseException
import me.retrodaredevil.solarthing.type.alter.StoredAlterPacket

class AlterUpdater(
        private val database: SolarThingDatabase,
        private val alterHandler: AlterHandler,
        private val sourceId: String,
) : Runnable {
    override fun run() {
        try {
            val packets: List<VersionedPacket<StoredAlterPacket>> = database.alterDatabase.queryAll(sourceId)
            alterHandler.update(packets)
        } catch (ex: SolarThingDatabaseException) {
            ex.printStackTrace()
        }
    }
}