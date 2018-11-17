package me.retrodaredevil.solarthing.android

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.request.DataRequester
import me.retrodaredevil.solarthing.android.request.DatabaseDataRequester
import java.util.*

const val UPDATE_PERIOD: Long = 1000 * 30

class PersistentService: Service(){
    private var timer: Timer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("Starting service")
        if(timer == null){
            timer = Timer()
        }
        timer?.scheduleAtFixedRate(object : TimerTask(){
            var successfulDataRequest: DataRequest? = null
            override fun run(){
                println("Updating")
                DataUpdaterTask(DatabaseDataRequester { GlobalData.connectionProperties }) { dataRequest ->
                    if(dataRequest.successful) {
                        println("Got successful data request")
                        successfulDataRequest = dataRequest
                        NotificationHandler.updateStatusNotification(this@PersistentService, dataRequest)
                    } else {
                        println("Got unsuccessful data request")
                        val nullableDataRequest = successfulDataRequest
                        if(nullableDataRequest != null) {
                            NotificationHandler.updateStatusNotification(this@PersistentService, nullableDataRequest)
                        }
                    }
                }.execute()
            }
        }, 1000L, UPDATE_PERIOD)
        println("finished starting service")
//        return super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        println("NOT Stopping service HAHA")
//        timer?.cancel()
        return super.stopService(name)
    }

}
private class DataUpdaterTask(
    private val dataRequester: DataRequester,
    private val updateNotification: (dataRequest: DataRequest) -> Unit
) : AsyncTask<Void, Void, DataRequest>() {
    override fun doInBackground(vararg params: Void?): DataRequest {
        return dataRequester.requestData()
    }

    override fun onPostExecute(result: DataRequest?) {
        if(result == null){
            throw NullPointerException("result is null!")
        }
        println("Received result: $result")
        updateNotification(result)
    }

}
