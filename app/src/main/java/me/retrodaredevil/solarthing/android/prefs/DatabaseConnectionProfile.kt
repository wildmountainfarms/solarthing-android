package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty

//@JsonSubTypes(
//    JsonSubTypes.Type(CouchDbDatabaseConnectionProfile::class)
//)
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = SaveKeys.connectionType)
interface DatabaseConnectionProfile {
    @get:JsonProperty(SaveKeys.connectionType)
    val connectionType: DatabaseConnectionType
}