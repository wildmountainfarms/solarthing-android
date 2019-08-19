package me.retrodaredevil.solarthing.android.prefs

import java.util.*

interface ProfileManager<T> {
    val activeProfile: T
    val activeProfileName: String

    var activeUUID: UUID

    val profileUUIDs: List<UUID>


    fun removeProfile(uuid: UUID): Boolean
//    fun addProfile(uuid: UUID, profile: T)
    fun addAndCreateProfile(name: String): Pair<UUID, T>

    fun setProfileName(uuid: UUID, name: String)
    fun getProfileName(uuid: UUID): String

    fun getProfile(uuid: UUID): T
}