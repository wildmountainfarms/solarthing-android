package me.retrodaredevil.solarthing.android.request

interface DataRequester {
    val currentlyUpdating: Boolean
    /**
     * Calling this method will block so this should be run in a separate thread.
     * @return The [DataRequest] object that represents the data retrieved
     */
    fun requestData(): DataRequest
}

