package me.retrodaredevil.solarthing.android.prefs

interface ConnectionProfile {
    val databaseConnectionProfile: DatabaseConnectionProfile
    val networkSwitchingProfile: NetworkSwitchingProfile

    var initialRequestTimeSeconds: Int
    var subsequentRequestTimeSeconds: Int
    var requestWaitTimeSeconds: Int
}