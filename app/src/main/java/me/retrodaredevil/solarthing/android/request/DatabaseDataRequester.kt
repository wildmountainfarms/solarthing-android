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
    private val connectionPropertiesGetter: () -> CouchDbProperties,
    private val startKeyGetter: () -> Long = { System.currentTimeMillis() - 2 * 60 * 60 * 1000 }
) : DataRequester {
    override var currentlyUpdating = false
        private set

    /**
     * Takes over the current thread and runs until data is retrieved or an error occurred.
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
            for (jsonObject in client.view("packets/millis").startKey(startKeyGetter()).query(JsonObject::class.java)) {
                val packetCollection = PacketCollections.createFromJson(jsonObject.getAsJsonObject("value"))
                list.add(packetCollection)
            }
            println("Updated collections!")
            return DataRequest(list, true, "Request Successful")
        } catch(ex: CouchDbException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false, "Request Failed", ex.stackTrace.toString(), ex.message)
        } catch(ex: NullPointerException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false, "(Please report) NPE (Likely Parsing Error)", ex.stackTrace.toString(), ex.message)
        } catch(ex: Exception) {
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false, "(Please report) ${ex.javaClass.simpleName} (Unknown)", ex.stackTrace.toString(), ex.message)
        } finally {
            currentlyUpdating = false
        }
    }
}
