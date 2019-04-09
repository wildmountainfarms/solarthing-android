package me.retrodaredevil.solarthing.android.request

import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketCollections
import org.lightcouch.CouchDbClientAndroid
import org.lightcouch.CouchDbException
import org.lightcouch.CouchDbProperties
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.*

class CouchDbDataRequester(
    private val connectionPropertiesCreator: () -> CouchDbProperties,
    private val startKeyGetter: () -> Long = { System.currentTimeMillis() - 2 * 60 * 60 * 1000 }
) : DataRequester {

    @Volatile
    override var currentlyUpdating = false
        private set

    /**
     * Takes over the current thread and runs until data is retrieved or an error occurred.
     *
     * @throws IllegalStateException Thrown when [currentlyUpdating] is true
     * @return The [DataRequest] which holds data about if the request was successful or not
     */
    override fun requestData(): DataRequest {
        synchronized(this) {
            if (currentlyUpdating) {
                throw IllegalStateException("The data is currently being updated!")
            }
            currentlyUpdating = true
        }
        var couchDbProperties: CouchDbProperties? = null
        try {
            couchDbProperties = connectionPropertiesCreator()
            val client = CouchDbClientAndroid(couchDbProperties)
            println("Successfully connected!")
            val list = ArrayList<PacketCollection>()
            for (jsonObject in client.view("packets/millis").startKey(startKeyGetter()).query(JsonObject::class.java)) {
                val packetCollection = PacketCollections.createFromJson(jsonObject.getAsJsonObject("value"))
                list.add(packetCollection)
            }
            println("Updated collections!")
            return DataRequest(list, true, "Request Successful", getAuthDebug(couchDbProperties))
        } catch(ex: CouchDbException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false,
                "Request Failed", getStackTrace(ex), ex.message, getAuthDebug(couchDbProperties))
        } catch(ex: NullPointerException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false,
                "(Please report) NPE (Likely Parsing Error)", getStackTrace(ex), ex.message, getAuthDebug(couchDbProperties))
        } catch(ex: Exception) {
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false,
                "(Please report) ${ex.javaClass.simpleName} (Unknown)", getStackTrace(ex), ex.message, getAuthDebug(couchDbProperties))
        } finally {
            currentlyUpdating = false
        }
    }
    private fun getAuthDebug(couchDbProperties: CouchDbProperties?): String?{
        return if(couchDbProperties!= null)
            "Properties: ${couchDbProperties.protocol} ${couchDbProperties.host}:${couchDbProperties.port} ${couchDbProperties.username} ${couchDbProperties.dbName}\n"
            else null
    }
    private fun getStackTrace(throwable: Throwable): String{
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}
