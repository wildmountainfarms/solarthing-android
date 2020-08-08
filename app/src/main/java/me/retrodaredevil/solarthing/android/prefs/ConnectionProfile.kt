package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class ConnectionProfile(
        @JsonProperty(SaveKeys.databaseConnectionProfile)
        @JsonDeserialize(`as` = CouchDbDatabaseConnectionProfile::class)
        val databaseConnectionProfile: DatabaseConnectionProfile = CouchDbDatabaseConnectionProfile(),
        @JsonProperty(SaveKeys.networkSwitchingProfile)
        val networkSwitchingProfile: NetworkSwitchingProfile = NetworkSwitchingProfile(),
        @JsonProperty(SaveKeys.initialRequestTimeSeconds)
        val initialRequestTimeSeconds: Int = DefaultOptions.initialRequestTimeSeconds,
        @JsonProperty(SaveKeys.subsequentRequestTimeSeconds)
        val subsequentRequestTimeSeconds: Int = DefaultOptions.subsequentRequestTimeSeconds,
        @JsonProperty(SaveKeys.preferredSourceId)
        val preferredSourceId: String? = DefaultOptions.preferredSourceId
) {
    fun selectSourceId(sourceIdCollection: Collection<String>): String {
        if (preferredSourceId != null && preferredSourceId in sourceIdCollection) {
            return preferredSourceId
        }
        return sourceIdCollection.first()
    }
}