package me.retrodaredevil.solarthing.android.prefs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

//@JsonSubTypes(
//    JsonSubTypes.Type(CouchDbDatabaseConnectionProfile::class)
//)
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "connectionType")
interface DatabaseConnectionProfile {
    @get:JsonProperty(SaveKeys.connectionType)
    val connectionType: DatabaseConnectionType
}