package me.retrodaredevil.solarthing.android

import org.lightcouch.CouchDbProperties

object GlobalData {
    lateinit var connectionProperties: CouchDbProperties
    var generatorFloatTimeMillis: Long = (1.5 * 60 * 60 * 1000).toLong()
    var generatorNotifyIntervalMillis: Long = (5 * 60 * 1000).toLong()
}