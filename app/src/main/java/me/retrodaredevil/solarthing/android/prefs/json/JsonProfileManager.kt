package me.retrodaredevil.solarthing.android.prefs.json

import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import java.util.*
import kotlin.NoSuchElementException

class JsonProfileManager<T>(
    private val jsonSaver: JsonSaver,
    private val newProfileJsonCreator: () -> JsonObject,
    private val profileCreator: (JsonSaver) -> T
) : ProfileManager<T> {
    override fun getProfileName(uuid: UUID): String {
        return (getJsonProfile(uuid) ?: throw NoSuchElementException("No such profile with uuid: $uuid"))["name"].asString
    }

    override val profileUUIDs: List<UUID>
        get() {
            val array = jsonSaver.jsonObject["profiles"].asJsonArray
            val r = mutableListOf<UUID>()
            for(element in array){
                val jsonObject = element.asJsonObject
                r.add(UUID.fromString(jsonObject["uuid"].asString))
            }
            return r
        }
    override var activeUUID: UUID
        get() = UUID.fromString(jsonSaver.jsonObject["active"].asString)
        set(value) {
            jsonSaver.jsonObject.addProperty("active", value.toString())
            jsonSaver.save()
        }
    override val activeProfile: T
        get() = getProfile(activeUUID) ?: error("No profile with the active UUID!")
    override val activeProfileName: String
        get() = (getJsonProfile(activeUUID) ?: error("No json profile with active UUID!"))["name"].asString

    override fun removeProfile(uuid: UUID): Boolean {
        val array = jsonSaver.jsonObject["profiles"].asJsonArray
        for(element in array){
            val uuidString = element.asJsonObject["uuid"].asString
            if(uuidString == uuid.toString()){
                array.remove(element)
                jsonSaver.save()
                return true
            }
        }
        return false
    }
    private fun getJsonProfile(uuid: UUID): JsonObject?{
        val uuidString = uuid.toString()
        val array = jsonSaver.jsonObject["profiles"].asJsonArray
        for(element in array){
            val jsonObject = element.asJsonObject
            if(jsonObject["uuid"].asString == uuidString){
                return jsonObject
            }
        }
        return null
    }

    override fun addAndCreateProfile(name: String): Pair<UUID, T> {
        val uuid = UUID.randomUUID()
        val array = jsonSaver.jsonObject["profiles"].asJsonArray
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", name)
        jsonObject.addProperty("uuid", uuid.toString())
        val profileObject = newProfileJsonCreator()
        jsonObject.add("profile", profileObject)
        array.add(jsonObject)
        jsonSaver.save()

        val profile = profileCreator(NestedJsonSaver({ getJsonProfile(uuid) ?: error("No profile with uuid: $uuid") }, jsonSaver::save))
        return Pair(uuid, profile)
    }

    override fun setProfileName(uuid: UUID, name: String) {
        getJsonProfile(uuid)?.addProperty("name", name) ?: throw NoSuchElementException("No such uuid: $uuid")
        jsonSaver.save()
    }

    override fun getProfile(uuid: UUID): T {
        // TODO we might be able to cache created profiles
        return profileCreator(NestedJsonSaver({getJsonProfile(uuid) ?: throw NoSuchElementException("No such uuid: $uuid")}, jsonSaver::save))
    }

}