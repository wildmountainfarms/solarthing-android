package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class ConnectionProfile(
    @JsonProperty(SaveKeys.databaseConnectionProfile, required = true)
    @JsonDeserialize(`as` = CouchDbDatabaseConnectionProfile::class) // TODO we can eventually remove this in a few versions once everyone is updated
    val databaseConnectionProfile: DatabaseConnectionProfile,
    @JsonProperty(SaveKeys.networkSwitchingProfile, required = true)
    val networkSwitchingProfile: NetworkSwitchingProfile,
    @JsonProperty(SaveKeys.initialRequestTimeSeconds, required = true)
    val initialRequestTimeSeconds: Int,
    @JsonProperty(SaveKeys.subsequentRequestTimeSeconds, required = true)
    val subsequentRequestTimeSeconds: Int
//    val requestWaitTimeSeconds: Int
)