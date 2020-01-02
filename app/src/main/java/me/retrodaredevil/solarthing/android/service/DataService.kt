package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.android.request.DataRequest

/**
 * An interface representing different services
 *
 * All method calls will be run on the UI thread so they cannot block.
 */
interface DataService {
    /**
     * Will only ever be called once.
     */
    fun onInit()

    /**
     * May be called multiple times
     */
    fun onCancel()

    /**
     * Only called once. Once this is called, it is expected that this object will not function again
     *
     * [onCancel] will be called before this is called, so do not worry about calling it again
     */
    fun onEnd()

    fun onNewDataRequestLoadStart()
    fun onDataRequest(dataRequest: DataRequest)
    fun onTimeout()

    val updatePeriodType: UpdatePeriodType
    val startKey: Long

    val shouldUpdate: Boolean
}
