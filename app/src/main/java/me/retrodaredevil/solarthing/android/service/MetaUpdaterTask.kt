package me.retrodaredevil.solarthing.android.service

import android.os.AsyncTask
import com.fasterxml.jackson.core.JsonProcessingException
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdbjava.exception.CouchDbException
import me.retrodaredevil.couchdbjava.json.jackson.CouchDbJacksonUtil
import me.retrodaredevil.solarthing.SolarThingConstants
import me.retrodaredevil.solarthing.android.util.createCouchDbInstance
import me.retrodaredevil.solarthing.android.util.createDefaultObjectMapper
import me.retrodaredevil.solarthing.misc.common.meta.DataMetaPacket
import me.retrodaredevil.solarthing.solar.outback.fx.meta.FXChargingSettingsPacket
import me.retrodaredevil.solarthing.type.closed.meta.DefaultMetaDatabase
import me.retrodaredevil.solarthing.type.closed.meta.DeviceInfoPacket
import me.retrodaredevil.solarthing.type.closed.meta.RootMetaPacket
import me.retrodaredevil.solarthing.type.closed.meta.TargetMetaPacket
import me.retrodaredevil.solarthing.util.JacksonUtil


class MetaUpdaterTask(
        private val couchProperties: CouchProperties,
        private val metaHandler: MetaHandler
) : AsyncTask<Void, Void, RootMetaPacket?>() {
    companion object {
        val MAPPER = createDefaultObjectMapper().apply {
            subtypeResolver.registerSubtypes(
                    TargetMetaPacket::class.java,
                    DeviceInfoPacket::class.java,
                    DataMetaPacket::class.java,
                    FXChargingSettingsPacket::class.java
            )
            JacksonUtil.lenientSubTypeMapper(this)
        }
    }
    override fun doInBackground(vararg params: Void?): RootMetaPacket? {
        val instance = createCouchDbInstance(couchProperties)
        val database = instance.getDatabase(SolarThingConstants.CLOSED_DATABASE)
        val documentData = try {
            database.getDocument("meta")
        } catch (ex: CouchDbException) {
            ex.printStackTrace()
            return null
        }

        return try {
            CouchDbJacksonUtil.readValue(MAPPER, documentData.jsonData, RootMetaPacket::class.java)
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
            return null
        }
    }

    override fun onPostExecute(result: RootMetaPacket?) {
        if (result != null) {
            metaHandler.metaDatabase = DefaultMetaDatabase(result)
            println("Updated meta")
        }
    }

}
