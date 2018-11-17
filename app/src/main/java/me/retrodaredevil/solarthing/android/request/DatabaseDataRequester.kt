package me.retrodaredevil.solarthing.android.request

import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketCollections
import org.lightcouch.CouchDbClientAndroid
import org.lightcouch.CouchDbException
import org.lightcouch.CouchDbProperties
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.*

class DatabaseDataRequester(
    private val connectionPropertiesGetter: () -> CouchDbProperties
) : DataRequester {
    override var currentlyUpdating = false
        private set

    /**
     * Takes over the current thread and runs until data is retreived or an error occured.
     *
     * @throws IllegalStateException Thrown when [currentlyUpdating] is true
     * @return The [DataRequest] which holds data about if the request was successful or not
     */
    override fun requestData(): DataRequest {
        if(currentlyUpdating){
            throw IllegalStateException("The data is currently being updated!")
        }
        try {
            currentlyUpdating = true
            val client = CouchDbClientAndroid(connectionPropertiesGetter())
            println("Successfully connected!")
            val list = ArrayList<PacketCollection>()
            for (jsonObject in client.view("packets/millis")
                .startKey(System.currentTimeMillis() - 1000 * 60 * 60)
                .query(JsonObject::class.java)) {
                val packetCollection = PacketCollections.createFromJson(jsonObject.getAsJsonObject("value"))
                list.add(packetCollection)
            }
            println("Updated collections!")
            return DataRequest(list, true, "Request Successful", null)
        } catch(ex: CouchDbException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false, "Request Failed", ex.stackTrace.toString())
        } catch(ex: NullPointerException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false, "NPE (Likely Parsing Error)", ex.stackTrace.toString())
        } catch(ex: Exception) {
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false, "Unknown Error: ${ex.javaClass.simpleName}", ex.stackTrace.toString())
        } finally {
            currentlyUpdating = false
        }
    }
}
