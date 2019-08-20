package me.retrodaredevil.solarthing.android.prefs

import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdb.CouchPropertiesBuilder

interface CouchDbDatabaseConnectionProfile : DatabaseConnectionProfile {
    override val connectionType: DatabaseConnectionType
        get() = DatabaseConnectionType.COUCHDB

    var protocol: String
    var host: String
    var port: Int
    var username: String
    var password: String
    var useAuth: Boolean

    fun createCouchProperties(): List<CouchProperties> {
        val username: String?
        val password: String?
        if(useAuth){
            username = this.username
            password = this.password
        } else {
            username = null
            password = null
        }
        val protocol = protocol
        val port = port
        val hostsString = host
        val hosts = hostsString.split(",")

        return hosts.map {
            CouchPropertiesBuilder(
                null,
                false,
                protocol,
                it,
                port,
                username,
                password
            ).build()
        }
    }
}