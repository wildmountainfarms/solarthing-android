package me.retrodaredevil.solarthing.android.prefs.gson

import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import java.util.*
import kotlin.NoSuchElementException

@Deprecated("This is part of the old prefs code")
class JsonProfileManager<T>(
    private val jsonSaver: JsonSaver,
    private val newProfileJsonCreator: () -> JsonObject,
    private val profileCreator: (JsonSaver) -> T
) : ProfileManager<T> {

    private val profileMapCache: MutableMap<UUID, T> = HashMap()

    override fun getProfileName(uuid: UUID): String {
        return (getJsonProfile(uuid, reload = true) ?: throw NoSuchElementException("No such profile with uuid: $uuid"))["name"].asString
    }

    override val profileUUIDs: List<UUID>
        get() {
            val array = jsonSaver.reloadedJsonObject["profiles"].asJsonArray
            val r = mutableListOf<UUID>()
            for(element in array){
                val jsonObject = element.asJsonObject
                r.add(UUID.fromString(jsonObject["uuid"].asString))
            }
            return r
        }
    override var activeUUID: UUID
        get() = UUID.fromString(jsonSaver.reloadedJsonObject["active"].asString)
        set(value) {
            println("Setting active UUID to: $value")
            jsonSaver["active"] = value.toString()
        }
    override val activeProfile: T
        get() = getProfile(activeUUID)
    override val activeProfileName: String
        get() = (getJsonProfile(activeUUID, reload = true) ?: error("No json profile with active UUID!"))["name"].asString

    override fun removeProfile(uuid: UUID): Boolean {
        val jsonObject = jsonSaver.reloadedJsonObject
        val array = jsonObject["profiles"].asJsonArray
        for(element in array){
            val uuidString = element.asJsonObject["uuid"].asString
            if(uuidString == uuid.toString()){
                val removed = array.remove(element)
                if(!removed){
                    throw IllegalStateException("Wasn't able to remove uuid: $uuid from array!")
                }
                val success = profileMapCache.remove(uuid) != null
                if(!success) {
                    throw IllegalStateException("A profile was not cached in the map with uuid: $uuid")
                }
                println("removing: uuid: $uuid")
                reloadProfiles(reload = false)
                if(jsonObject["active"].asString!! == uuidString){ // we are removing the active uuid
                    jsonObject.addProperty("active", profileMapCache.keys.firstOrNull()?.toString() ?: error("You cannot remove the last profile! There must be at least one profile!"))
                }
                jsonSaver.save()
                return true
            }
        }
        return false
    }
    private fun getJsonProfile(uuid: UUID, reload: Boolean = false): JsonObject?{
        if(reload){
            jsonSaver.reload()
        }
        val array = jsonSaver.jsonObject["profiles"].asJsonArray
        val uuidString = uuid.toString()
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
        val array = jsonSaver.reloadedJsonObject["profiles"].asJsonArray
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", name)
        jsonObject.addProperty("uuid", uuid.toString())
        println("adding: name: $name uuid: $uuid")
        val profileObject = newProfileJsonCreator()
        jsonObject.add("profile", profileObject)
        array.add(jsonObject)
        jsonSaver.save()

        val profile = profileCreator(NestedJsonSaver({ getJsonProfile(uuid)?.getAsJsonObject("profile") ?: error("No profile with uuid: $uuid") }, jsonSaver::reload, jsonSaver::save))
        profileMapCache[uuid] = profile
        return Pair(uuid, profile)
    }
    private fun reloadProfiles(reload: Boolean = true){
        if(reload){
            jsonSaver.reload()
        }
        val array = jsonSaver.jsonObject["profiles"].asJsonArray
//        println(array)
        for(element in array){
            val jsonObject = element.asJsonObject
            val uuid = UUID.fromString(jsonObject["uuid"].asString)
            if(uuid !in profileMapCache){
                val profile = profileCreator(NestedJsonSaver({ getJsonProfile(uuid)?.getAsJsonObject("profile") ?: error("No profile with uuid: $uuid") }, jsonSaver::reload, jsonSaver::save))
                profileMapCache[uuid] = profile
            }
        }
    }

    override fun setProfileName(uuid: UUID, name: String) {
        getJsonProfile(uuid, reload = true)?.addProperty("name", name) ?: throw NoSuchElementException("No such uuid: $uuid map: $profileMapCache")
        jsonSaver.save()
    }

    override fun getProfile(uuid: UUID): T {
        return profileMapCache[uuid] ?: run {
            reloadProfiles()
            profileMapCache[uuid] ?: throw NoSuchElementException("No such uuid: $uuid map: $profileMapCache")
        }
    }

}