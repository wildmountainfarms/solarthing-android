package me.retrodaredevil.solarthing.android.prefs

import java.util.*

interface ProfileManager<T> : ProfileProvider<T>{
    override val activeProfile: T
    val activeProfileName: String

    var activeUUID: UUID

    val profileUUIDs: List<UUID>


    /**
     * @param uuid The [UUID] of the profile to remove. If this is the same as [activeUUID], [activeUUID] (and [activeProfile], etc) will become another [UUID] from [profileUUIDs]. It is not defined how the new profile is determined.
     * @return true if The profile was removed, false otherwise
     */
    fun removeProfile(uuid: UUID): Boolean
    fun addAndCreateProfile(name: String): Pair<UUID, T>

    fun setProfileName(uuid: UUID, name: String)
    fun getProfileName(uuid: UUID): String

    fun getProfile(uuid: UUID): T
}