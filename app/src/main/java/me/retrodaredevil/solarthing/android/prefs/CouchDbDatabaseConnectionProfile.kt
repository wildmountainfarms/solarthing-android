package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdb.CouchPropertiesBuilder

//@JsonTypeName("COUCHDB")
@JsonIgnoreProperties(SaveKeys.connectionType)
data class CouchDbDatabaseConnectionProfile(
        @JsonProperty(SaveKeys.CouchDb.protocol)
        val protocol: String = DefaultOptions.CouchDb.protocol,
        @JsonProperty(SaveKeys.CouchDb.host)
        val host: String = DefaultOptions.CouchDb.host,
        @JsonProperty(SaveKeys.CouchDb.port)
        val port: Int = DefaultOptions.CouchDb.port,
        @JsonProperty(SaveKeys.CouchDb.username)
        val username: String = DefaultOptions.CouchDb.username,
        @JsonProperty(SaveKeys.CouchDb.password)
        val password: String = DefaultOptions.CouchDb.password,
        @JsonProperty(SaveKeys.CouchDb.useAuth)
        val useAuth: Boolean = DefaultOptions.CouchDb.useAuth
) : DatabaseConnectionProfile {
    override val connectionType: DatabaseConnectionType
        get() = DatabaseConnectionType.COUCHDB


    fun createCouchProperties(): CouchProperties {
        val username: String?
        val password: String?
        if(useAuth){
            username = this.username
            password = this.password
        } else {
            username = null
            password = null
        }

        return CouchPropertiesBuilder(
                protocol,
                host,
                port,
                username,
                password
        ).build()
    }
}
