package me.retrodaredevil.solarthing.android.service

import android.os.AsyncTask
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdb.CouchPropertiesBuilder
import me.retrodaredevil.solarthing.SolarThingConstants
import me.retrodaredevil.solarthing.android.util.createDefaultObjectMapper
import me.retrodaredevil.solarthing.android.util.createHttpClient
import me.retrodaredevil.solarthing.meta.DefaultMetaDatabase
import me.retrodaredevil.solarthing.meta.DeviceInfoPacket
import me.retrodaredevil.solarthing.meta.RootMetaPacket
import me.retrodaredevil.solarthing.meta.TargetMetaPacket
import me.retrodaredevil.solarthing.misc.common.meta.DataMetaPacket
import me.retrodaredevil.solarthing.solar.outback.fx.meta.FXChargingSettingsPacket
import me.retrodaredevil.solarthing.util.JacksonUtil
import org.ektorp.DbAccessException
import org.ektorp.impl.StdCouchDbConnector
import org.ektorp.impl.StdCouchDbInstance


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
        val httpClient = createHttpClient(
                CouchPropertiesBuilder(couchProperties)
                        .setConnectionTimeoutMillis(10_000)
                        .setSocketTimeoutMillis(Int.MAX_VALUE)
                        .build()
        )
        val instance = StdCouchDbInstance(httpClient)
        val client = StdCouchDbConnector(SolarThingConstants.CLOSED_UNIQUE_NAME, instance)
        val jsonNode = try {
            client.find(JsonNode::class.java, "meta")
        } catch (ex: DbAccessException) {
            ex.printStackTrace()
            return null
        }

        return try {
            MAPPER.treeToValue(jsonNode, RootMetaPacket::class.java)
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
