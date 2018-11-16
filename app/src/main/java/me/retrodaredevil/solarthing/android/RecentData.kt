package me.retrodaredevil.solarthing.android

import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketCollections
import org.lightcouch.CouchDbClientAndroid
import org.lightcouch.CouchDbException
import org.lightcouch.CouchDbProperties
import java.util.*

object RecentData {
    lateinit var connectionProperties: CouchDbProperties
    // CouchDbProperties("solarthing", false, "http", "192.168.1.203", 5984, "admin", "relax")
    var packetCollections: List<PacketCollection> = Collections.emptyList()
        private set

    fun updateData(): Boolean{
        try {
            val client = CouchDbClientAndroid(connectionProperties)
            println("Successfully connected!")
            val list = ArrayList<PacketCollection>()
            for (jsonObject in client.view("packets/millis")
                .startKey(System.currentTimeMillis() - 1000 * 60 * 60)
                .query(JsonObject::class.java)) {
                val packetCollection = PacketCollections.createFromJson(jsonObject.getAsJsonObject("value"))
                list.add(packetCollection)
            }
            packetCollections = list
            println("Updated collections!")
            return true
        } catch(ex: CouchDbException){
            ex.printStackTrace()
            println("Couldn't update data!")
            return false
        }
    }
}