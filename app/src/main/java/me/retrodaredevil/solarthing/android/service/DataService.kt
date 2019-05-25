package me.retrodaredevil.solarthing.android.service

import me.retrodaredevil.solarthing.android.request.DataRequest

interface DataService {
    fun onInit()
    fun onEnd()

    fun onNewDataRequestLoadStart()
    fun onDataRequest(dataRequest: DataRequest)
    fun onTimeout()

    val updatePeriodType: UpdatePeriodType
    val startKey: Long
}