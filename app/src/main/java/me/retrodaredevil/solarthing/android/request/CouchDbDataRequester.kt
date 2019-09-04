package me.retrodaredevil.solarthing.android.request

import com.google.gson.JsonObject
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.solarthing.android.PacketGroup
import me.retrodaredevil.solarthing.android.PacketParse
import me.retrodaredevil.solarthing.android.parsePacketGroup
import me.retrodaredevil.solarthing.packets.Packet
import org.lightcouch.CouchDbClientAndroid
import org.lightcouch.CouchDbException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.SocketException
import java.util.*


class CouchDbDataRequester(
    private val connectionPropertiesCreator: () -> CouchProperties,
    private val jsonPacketGetter: (JsonObject) -> Packet,
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
        var couchProperties: CouchProperties? = null
        try {
            couchProperties = connectionPropertiesCreator()
            val client = CouchDbClientAndroid(couchProperties.createProperties())

            println("Successfully connected!")
            val list = ArrayList<PacketGroup>()
            var exception: Exception? = null
            for (jsonObject in client.view("packets/millis").startKey(startKeyGetter()).query(JsonObject::class.java)) {
                val packetCollection = jsonObject.getAsJsonObject("value")
                when(val packetParse = parsePacketGroup(packetCollection, jsonPacketGetter)){
                    is PacketParse.Success -> list.add(packetParse.packetGroup)
                    is PacketParse.Failure -> exception = packetParse.exception
                }
            }
            if(list.isEmpty() && exception != null){
                return DataRequest(
                    emptyList(),
                    false, "Got all unknown packets",
                    couchProperties.host,
                    getStackTrace(exception),
                    exception.message,
                    getAuthDebug(couchProperties)
                )
            }
            exception?.printStackTrace()
            println("Updated collections!")
            return DataRequest(list, true, "Request Successful", couchProperties.host, authDebug = getAuthDebug(couchProperties))
        } catch(ex: CouchDbException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false,
                "Request Failed", couchProperties?.host, getStackTrace(ex), ex.message, getAuthDebug(couchProperties))
        } catch(ex: SocketException){
            return DataRequest(Collections.emptyList(), false,
                "Request Cut Off", couchProperties?.host, getStackTrace(ex), ex.message, getAuthDebug(couchProperties))
        } catch(ex: NullPointerException){
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false,
                "(Please report) NPE (Likely Parsing Error)", couchProperties?.host, getStackTrace(ex), ex.message, getAuthDebug(couchProperties))
        } catch(ex: Exception) {
            ex.printStackTrace()
            return DataRequest(Collections.emptyList(), false,
                "(Please report) ${ex.javaClass.simpleName} (Unknown)", couchProperties?.host, getStackTrace(ex), ex.message, getAuthDebug(couchProperties))
        } finally {
            currentlyUpdating = false
        }
    }
    private fun getAuthDebug(couchProperties: CouchProperties?): String?{
        return if(couchProperties!= null)
            "Properties: ${couchProperties.protocol} ${couchProperties.host}:${couchProperties.port} ${couchProperties.username} ${couchProperties.database}\n"
            else null
    }
    private fun getStackTrace(throwable: Throwable): String{
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}
