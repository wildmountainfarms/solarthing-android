package me.retrodaredevil.solarthing.android.service

import android.app.Service
import me.retrodaredevil.solarthing.android.request.DataRequest

@Suppress("unused")
class CommandFeedbackService(
    private val service: Service
) : DataService {
    override fun onInit() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCancel() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onEnd() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNewDataRequestLoadStart() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDataRequest(dataRequest: DataRequest) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTimeout() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val updatePeriodType = UpdatePeriodType.SMALL_DATA
    override val startKey: Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val shouldUpdate: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

}