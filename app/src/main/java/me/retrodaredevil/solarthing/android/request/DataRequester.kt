package me.retrodaredevil.solarthing.android.request

interface DataRequester {
    val currentlyUpdating: Boolean
    fun requestData(): DataRequest
}

