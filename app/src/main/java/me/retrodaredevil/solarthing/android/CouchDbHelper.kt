package me.retrodaredevil.solarthing.android

import org.lightcouch.CouchDbProperties

fun CouchDbProperties.clone(): CouchDbProperties {
    return CouchDbProperties(this.dbName, this.isCreateDbIfNotExist, this.protocol, this.host, this.port, this.username, this.password)
}