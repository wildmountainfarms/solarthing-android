package me.retrodaredevil.solarthing.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.request.DataRequester
import me.retrodaredevil.solarthing.android.request.DatabaseDataRequester
import java.util.*

const val UPDATE_PERIOD: Long = 1000 * 24
const val NOTIFICATION_ID: Int = 1

class PersistentService: Service(){
    private var timer: Timer? = null
    private var task: AsyncTask<*, *, *>? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        setToLoadingNotification()
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
                task = DataUpdaterTask(DatabaseDataRequester { GlobalData.connectionProperties }) { dataRequest ->
                    val usedRequest: DataRequest?
                    if(dataRequest.successful) {
                        println("Got successful data request")
                        successfulDataRequest = dataRequest
                        usedRequest = dataRequest
                    } else {
                        println("Got unsuccessful data request")
                        usedRequest = successfulDataRequest
                    }
                    if(usedRequest != null) {
                        val notification = NotificationHandler.createStatusNotification(
                            this@PersistentService,
                            usedRequest.packetCollectionList
                        )
                        if(notification != null) {
                            notify(notification)
                        } else {
                            setToLoadingNotification()
                        }
                    } else {
                        setToLoadingNotification()
                    }
                }.execute()
            }
        }, 1000L, UPDATE_PERIOD)
        println("finished starting service")
//        return super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
    private fun setToLoadingNotification(){
        val notification = getBuilder()
            .setOngoing(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setContentText("Loading Solar Data")
            .setProgress(2, 1, true)
            .build()
        notify(notification)
    }
    private fun notify(notification: Notification){
        getManager().notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }
    private fun getBuilder(): Notification.Builder {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return Notification.Builder(this, NotificationChannels.PERSISTENT_STATUS.id)
        }
        return Notification.Builder(this)
    }
    private fun getManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun stopService(name: Intent?): Boolean {
        timer?.cancel()
        task?.cancel(true)
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
