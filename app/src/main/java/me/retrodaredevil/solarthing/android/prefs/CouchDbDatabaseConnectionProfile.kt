package me.retrodaredevil.solarthing.android.prefs

interface CouchDbDatabaseConnectionProfile : DatabaseConnectionProfile {
    override val connectionType: DatabaseConnectionType
        get() = DatabaseConnectionType.COUCHDB

    var protocol: String
    var host: String
    var port: Int
    var username: String
    var password: String
    var useAuth: Boolean
}