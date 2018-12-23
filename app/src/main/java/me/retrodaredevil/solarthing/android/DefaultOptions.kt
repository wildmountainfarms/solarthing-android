package me.retrodaredevil.solarthing.android

object DefaultOptions {
    object CouchDb {
        val port = 5984
        val databaseName = "solarthing"
        val host = "192.168.1.110"
        val username = "admin"
        val password = "relax"
        val protocol = "http"
    }

    val generatorFloatTimeHours = 1.5F

    val generatorNotifyIntervalMillis = 5 * 60 * 1000L

    val initialRequestTimeSeconds = 60

    val subsequentRequestTimeSeconds = 30
}