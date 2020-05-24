package me.retrodaredevil.solarthing.android.prefs.saving

import com.fasterxml.jackson.annotation.JsonIgnore
import me.retrodaredevil.solarthing.android.util.createDefaultObjectMapper
import me.retrodaredevil.solarthing.android.prefs.ProfileHolder
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import java.util.*
import kotlin.NoSuchElementException

data class ProfileData<T>(
        val name: String,
        val uuid: UUID,
        val profile: T
)

data class ProfileManagerData<T>(
        val profiles: List<ProfileData<T>>,
        val active: UUID
) {
    @JsonIgnore
    fun getActiveProfileData(): ProfileData<T> = getProfileData(active)
    fun getProfileData(uuid: UUID): ProfileData<T> {
        return profiles.firstOrNull { it.uuid == uuid } ?: throw NoSuchElementException("No profile with uuid=$uuid. profiles: $profiles")
    }
}

class BasicProfileManager<T>
constructor(
        private val profileHolder: ProfileHolder<ProfileManagerData<T>>,
        private val defaultProfileCreator: () -> T
) : ProfileManager<T> {
    companion object {
        inline fun <reified T>createJacksonProfileManager(stringValueSaver: StringValueSaver, crossinline defaultProfileCreator: () -> T): BasicProfileManager<T> {
            val mapper = createDefaultObjectMapper()
            val type = mapper.typeFactory.constructParametricType(ProfileManagerData::class.java, T::class.java)
            return BasicProfileManager(JacksonProfileHolder(stringValueSaver, type, mapper) {
                val uuid = UUID.randomUUID()
                ProfileManagerData(listOf(ProfileData("Default Profile", uuid, defaultProfileCreator())), uuid)
            }) { defaultProfileCreator() }
        }
    }

    override fun getProfileName(uuid: UUID): String {
        return profileHolder.profile.profiles.firstOrNull { it.uuid == uuid}?.name ?: throw NoSuchElementException("No profile with uuid=$uuid")
    }

    override val profileUUIDs: List<UUID>
        get() = profileHolder.profile.profiles.map { it.uuid }
    override var activeUUID: UUID
        get() = profileHolder.profile.active
        set(value) {
            profileHolder.profile = profileHolder.profile.copy(active = value)
        }
    override val activeProfile: ProfileHolder<T>
        get() = BasicProfileHolder(profileHolder.profile.active)
    override val activeProfileName: String
        get() = profileHolder.profile.getActiveProfileData().name

    override fun removeProfile(uuid: UUID): Boolean {
        val old = profileHolder.profile
        val newList = mutableListOf<ProfileData<T>>()
        var removed = false
        for(profileData in old.profiles){
            if(profileData.uuid == uuid){
                removed = true
                continue
            }
            newList.add(profileData)
        }
        val active = if(uuid == old.active){
            newList.firstOrNull()?.uuid ?: throw IllegalStateException("Cannot remove active profile!")
        } else old.active
        profileHolder.profile = ProfileManagerData(newList, active)
        return removed
    }

    override fun addAndCreateProfile(name: String): Pair<UUID, ProfileHolder<T>> {
        val uuid = UUID.randomUUID()
        val profileData = ProfileData(name, uuid, defaultProfileCreator())
        val old = profileHolder.profile
        profileHolder.profile = ProfileManagerData(old.profiles + listOf(profileData), old.active)
        return Pair(uuid, BasicProfileHolder(uuid))
    }

    override fun setProfileName(uuid: UUID, name: String) {
        val old = profileHolder.profile
        profileHolder.profile = ProfileManagerData(
                old.profiles.map {
                    ProfileData(if(it.uuid == uuid) name else it.name, it.uuid, it.profile)
                },
                old.active
        )
    }

    override fun getProfile(uuid: UUID): ProfileHolder<T> {
        return BasicProfileHolder(uuid)
    }

    private inner class BasicProfileHolder(
            private val uuid: UUID
    ) : ProfileHolder<T> {
        override var profile: T
            get() = profileHolder.profile.getProfileData(uuid).profile
            set(value) {
                val old = profileHolder.profile
                profileHolder.profile = ProfileManagerData(
                        old.profiles.map {
                            val profile = if(it.uuid == uuid){
                                value
                            } else {
                                it.profile
                            }
                            ProfileData(it.name, it.uuid, profile)
                        },
                        old.active
                )
            }

    }
}
