package me.retrodaredevil.solarthing.android.prefs

interface ConnectionProfile {
    val databaseConnectionProfile: DatabaseConnectionProfile

    var initialRequestTimeSeconds: Int
    var subsequentRequestTimeSeconds: Int
    var requestWaitTimeSeconds: Int
}