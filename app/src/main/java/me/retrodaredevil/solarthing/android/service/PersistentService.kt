package me.retrodaredevil.solarthing.android.service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import com.google.gson.JsonObject
import me.retrodaredevil.solarthing.outhouse.OuthousePackets
import me.retrodaredevil.solarthing.packets.Packet
import me.retrodaredevil.solarthing.packets.collection.PacketCollections
import me.retrodaredevil.solarthing.solar.SolarPackets
import me.retrodaredevil.solarthing.android.MainActivity
import me.retrodaredevil.solarthing.android.Prefs
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.clone
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.PERSISTENT_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.getGroup
import me.retrodaredevil.solarthing.android.request.CouchDbDataRequester
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.request.DataRequester
import me.retrodaredevil.solarthing.android.request.DataRequesterMultiplexer


fun restartService(context: Context){
    val serviceIntent = Intent(context, PersistentService::class.java)
    context.stopService(serviceIntent)
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}
fun startServiceIfNotRunning(context: Context){
    if(isServiceRunning(context, PersistentService::class.java)){
        return
    }
    val serviceIntent = Intent(context, PersistentService::class.java)
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}
fun stopService(context: Context){
    val serviceIntent = Intent(context, PersistentService::class.java)
    context.stopService(serviceIntent)
}
private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Int.MAX_VALUE).any { serviceClass.name == it.service.className }
}
private const val STOP_SERVICE_ACTION = "me.retrodaredevil.solarthing.android.service.action.stop_service"
private const val RELOAD_SERVICE_ACTION = "me.retrodaredevil.solarthing.android.service.action.reload_service"
private const val RESTART_SERVICE_ACTION = "me.retrodaredevil.solarthing.android.service.action.restart_service"

private class ServiceObject(
    val dataService: DataService,
    val databaseName: String,
    val jsonPacketGetter: (JsonObject) -> Packet
){
    var task: AsyncTask<*, *, *>? = null

    var dataRequesters = emptyList<DataRequester>()
    val dataRequester = DataRequesterMultiplexer(
        this::dataRequesters
    )
}

class PersistentService : Service(), Runnable{
    private val prefs = Prefs(this)
    private val handler by lazy { Handler() }
    /** A Mutable Collection that is sorted from oldest to newest*/

    private val services = listOf(
        ServiceObject(OuthouseDataService(this), "outhouse", OuthousePackets::createFromJson),
        ServiceObject(SolarDataService(this, prefs), "solarthing", SolarPackets::createFromJson)
    )

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ShowToast")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        for(service in services){
            service.dataService.onInit()
        }
        handler.postDelayed(this, 300)
        Toast.makeText(this, "SolarThing Notification Service started", Toast.LENGTH_LONG).show()
        println("Starting service")
        updateNotification(System.currentTimeMillis() + 300)
        return START_STICKY
    }
    private fun updateNotification(countDownWhen: Long){
        // unanswered question with problem we're having here: https://stackoverflow.com/questions/47703216/android-clicking-grouped-notifications-restarts-app
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val builder = getBuilder()
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.horse)
            .setContentTitle("SolarThing service is running")
            .setContentText("${services.count { it.dataService.shouldUpdate }} service(s) are running")
            .setContentIntent(PendingIntent.getActivity(this, 0, mainActivityIntent, 0))
            .setWhen(1) // make it the lowest priority
            .setShowWhen(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(getGroup(PERSISTENT_NOTIFICATION_ID))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // countdown
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(countDownWhen)

            // buttons
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.horse),
                    "Stop",
                    PendingIntent.getBroadcast(
                        this, 0,
                        Intent(STOP_SERVICE_ACTION),
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                ).build()
            )
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.horse),
                    "Reload",
                    PendingIntent.getBroadcast(
                        this, 0,
                        Intent(RELOAD_SERVICE_ACTION),
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                ).build()
            )
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.horse),
                    "Restart",
                    PendingIntent.getBroadcast(
                        this, 0,
                        Intent(RESTART_SERVICE_ACTION),
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )

                ).build()
            )
            val intentFilter = IntentFilter()
            intentFilter.addAction(STOP_SERVICE_ACTION)
            intentFilter.addAction(RELOAD_SERVICE_ACTION)
            intentFilter.addAction(RESTART_SERVICE_ACTION)
            registerReceiver(receiver, intentFilter)
        }
        val notification = builder.build()
        getManager().notify(PERSISTENT_NOTIFICATION_ID, notification)
        startForeground(PERSISTENT_NOTIFICATION_ID, notification)
    }
    @SuppressWarnings("deprecated")
    private fun getBuilder(): Notification.Builder {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return Notification.Builder(this, NotificationChannels.PERSISTENT.id)
        }
        return Notification.Builder(this)
    }
    private fun getManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun run() {
        var needsLargeData = false
        for(service in services){
            val task = service.task
            if(task != null){
                service.task = null
                if(task.cancel(true)) { // if the task was still running then...
                    service.dataService.onTimeout()
                }
            }
            if(!service.dataService.shouldUpdate){
                service.dataService.onCancel()
                continue
            }

            service.dataRequesters = prefs.createCouchDbProperties().map{
                CouchDbDataRequester(
                    { it.clone().apply { dbName = service.databaseName }},
                    service.jsonPacketGetter,
                    { service.dataService.startKey }
                )
            }
            if(service.dataService.updatePeriodType == UpdatePeriodType.LARGE_DATA){
                needsLargeData = true
            }
            service.task = DataUpdaterTask(service.dataRequester, service.dataService::onDataRequest).execute()
        }

        val delay = if(needsLargeData){ prefs.initialRequestTimeSeconds * 1000L } else { prefs.subsequentRequestTimeSeconds * 1000L }
        handler.postDelayed(this, delay)
        updateNotification(System.currentTimeMillis() + delay)
    }
    private fun reload(){
        handler.removeCallbacks(this)
        handler.postDelayed(this, 100)
    }

    override fun onDestroy() {
        println("[123]Stopping persistent service")
        handler.removeCallbacks(this)
        unregisterReceiver(receiver)
        for(service in services){
            service.task?.cancel(true)
            service.dataService.onCancel()
            service.dataService.onEnd()
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(context != null && intent != null){
                when(intent.action){
                    STOP_SERVICE_ACTION -> stopService(context)
                    RELOAD_SERVICE_ACTION -> reload()
                    RESTART_SERVICE_ACTION -> restartService(context)
                    else -> println("unknown action: ${intent.action}")
                }
            }
        }

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

