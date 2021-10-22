package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.database.SolarThingDatabase
import me.retrodaredevil.solarthing.database.exception.SolarThingDatabaseException
import me.retrodaredevil.solarthing.type.closed.meta.DefaultMetaDatabase
import me.retrodaredevil.solarthing.type.closed.meta.RootMetaPacket


class MetaUpdater(
        private val database: SolarThingDatabase,
        private val metaHandler: MetaHandler
) : Runnable {

    private fun query(): RootMetaPacket? {
        return try {
            database.queryMetadata().packet
        } catch (ex: SolarThingDatabaseException) {
            ex.printStackTrace()
            null
        }
    }

    override fun run() {
        val result = query()
        if (result != null) {
            metaHandler.metaDatabase = DefaultMetaDatabase(result)
            println("Updated meta")
        }
    }
}
