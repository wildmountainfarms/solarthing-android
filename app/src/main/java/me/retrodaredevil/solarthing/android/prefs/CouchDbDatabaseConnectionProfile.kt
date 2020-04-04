package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdb.CouchPropertiesBuilder

//@JsonTypeName("COUCHDB")
@JsonIgnoreProperties(SaveKeys.connectionType)
class CouchDbDatabaseConnectionProfile(
    @JsonProperty(SaveKeys.CouchDb.protocol)
    val protocol: String,
    @JsonProperty(SaveKeys.CouchDb.host)
    val host: String,
    @JsonProperty(SaveKeys.CouchDb.port)
    val port: Int,
    @JsonProperty(SaveKeys.CouchDb.username)
    val username: String,
    @JsonProperty(SaveKeys.CouchDb.password)
    val password: String,
    @JsonProperty(SaveKeys.CouchDb.useAuth)
    val useAuth: Boolean
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
