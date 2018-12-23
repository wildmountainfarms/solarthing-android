package me.retrodaredevil.solarthing.android

import org.lightcouch.CouchDbProperties

object GlobalData {
    lateinit var connectionProperties: CouchDbProperties
    var generatorFloatTimeHours = DefaultOptions.generatorFloatTimeHours

    var initialRequestTimeSeconds = DefaultOptions.initialRequestTimeSeconds
    var subsequentRequestTimeSeconds = DefaultOptions.subsequentRequestTimeSeconds
}