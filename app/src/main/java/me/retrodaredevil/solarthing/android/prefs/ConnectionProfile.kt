package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class ConnectionProfile(
    @JsonProperty(SaveKeys.databaseConnectionProfile)
    @JsonDeserialize(`as` = CouchDbDatabaseConnectionProfile::class) // TODO we can eventually remove this in a few versions once everyone is updated
    val databaseConnectionProfile: DatabaseConnectionProfile = CouchDbDatabaseConnectionProfile(),
    @JsonProperty(SaveKeys.networkSwitchingProfile)
    val networkSwitchingProfile: NetworkSwitchingProfile = NetworkSwitchingProfile(),
    @JsonProperty(SaveKeys.initialRequestTimeSeconds)
    val initialRequestTimeSeconds: Int = DefaultOptions.initialRequestTimeSeconds,
    @JsonProperty(SaveKeys.subsequentRequestTimeSeconds)
    val subsequentRequestTimeSeconds: Int = DefaultOptions.subsequentRequestTimeSeconds
)