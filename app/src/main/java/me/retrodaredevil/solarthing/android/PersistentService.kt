package me.retrodaredevil.solarthing.android

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.request.CouchDbDataRequester
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.request.DataRequester
import me.retrodaredevil.solarthing.android.request.DataRequesterMultiplexer
import java.text.DateFormat
import java.util.*

const val NOTIFICATION_ID: Int = 1
const val GENERATOR_NOTIFICATION_ID: Int = 2

enum class UpdatePeriodType {
    LARGE_DATA, SMALL_DATA
}
fun restartService(context: Context){
    val serviceIntent = Intent("me.retrodaredevil.solarthing.android.PersistentService")
    serviceIntent.setClass(context, PersistentService::class.java)
    context.stopService(serviceIntent)
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

class PersistentService : Service(), Runnable{
    private val prefs = Prefs(this)
    private val handler by lazy { Handler() }
    /** A Mutable Collection that is sorted from oldest to newest*/
    private val packetInfoCollection: MutableCollection<PacketInfo> = TreeSet(Comparator { o1, o2 -> (o1.dateMillis - o2.dateMillis).toInt() })

//    private val dataRequesters: List<DataRequester> = listOf()
    private var dataRequesters = emptyList<DataRequester>()
    private val dataRequester = DataRequesterMultiplexer(
        this::dataRequesters
    )
//    private val dataRequester = CouchDbDataRequester(
//        prefs::createCouchDbProperties,
//        this::getStartKey
//    )

    private var task: AsyncTask<*, *, *>? = null
    private var lastGeneratorNotification: Long? = null
    private var updatePeriodType = UpdatePeriodType.LARGE_DATA

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ShowToast")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setToLoadingNotification()
        handler.postDelayed(this, 300)
        Toast.makeText(this, "SolarThing Notification Service Started", Toast.LENGTH_LONG).show()
        println("Starting service")
        return START_STICKY
    }

    override fun run() {
        if(task?.status == AsyncTask.Status.RUNNING){
            if(!doNotify(getTimedOutSummary(null))){
                setToTimedOut()
            }
        }
        task?.cancel(true)
        dataRequesters = prefs.createCouchDbProperties().map{ CouchDbDataRequester({it}, this::getStartKey)}

        task = DataUpdaterTask(dataRequester, this::onDataRequest).execute()
        when(updatePeriodType){
            UpdatePeriodType.LARGE_DATA -> {
                handler.postDelayed(this, prefs.initialRequestTimeSeconds * 1000L)
                updatePeriodType = UpdatePeriodType.SMALL_DATA
            }
            UpdatePeriodType.SMALL_DATA -> {
                handler.postDelayed(this, prefs.subsequentRequestTimeSeconds * 1000L)
            }
        }

    }
    private fun getStartKey(): Long = if(packetInfoCollection.isEmpty()){
        System.currentTimeMillis() - 2 * 60 * 60 * 1000
    } else {
        packetInfoCollection.last().dateMillis
    }
    private fun onDataRequest(dataRequest: DataRequest){
        val summary: String

        if(dataRequest.successful) {
            println("[123]Got successful data request")
            packetInfoCollection.addAll(dataRequest.packetCollectionList.map { PacketInfo(it) })
            summary = getConnectedSummary(dataRequest.host)
        } else {
            println("[123]Got unsuccessful data request")
            summary = getFailedSummary(dataRequest.host)
        }
        if(!doNotify(summary)){
            if(dataRequest.successful){
                setToNoData(dataRequest)
            } else {
                setToFailedNotification(dataRequest)
            }
        }
    }

    private fun doNotify(summary: String): Boolean{
        if(packetInfoCollection.isEmpty()){
            return false
        }
        val currentInfo = packetInfoCollection.last()
        var floatModeActivatedInfo: PacketInfo? = null
        val virtualFloatModeMinimumBatteryVoltage = prefs.virtualFloatModeMinimumBatteryVoltage
        for(info in packetInfoCollection.reversed()){ // go through latest packets first
            if(!info.isGeneratorInFloat(virtualFloatModeMinimumBatteryVoltage)){
                break
            }
            floatModeActivatedInfo = info // get the oldest packet where all the packets up to the current packet have float mode active (the packet where float mode started)
        }
        val notification = NotificationHandler.createStatusNotification(
            this@PersistentService,
            currentInfo,
            summary,
            floatModeActivatedInfo,
            (prefs.generatorFloatTimeHours * 60 * 60 * 1000).toLong()
        )
        notify(notification)

        if(floatModeActivatedInfo != null){
            // check to see if we should send a notification
            val generatorFloatTimeMillis = (prefs.generatorFloatTimeHours * 60 * 60 * 1000).toLong()
            val now = System.currentTimeMillis()
            if(floatModeActivatedInfo.dateMillis + generatorFloatTimeMillis < now) { // should it be turned off?
                val last = lastGeneratorNotification
                if (last == null || last + DefaultOptions.generatorNotifyIntervalMillis < now) {
                    getManager().notify(
                        GENERATOR_NOTIFICATION_ID,
                        NotificationHandler.createGeneratorAlert(
                            this@PersistentService,
                            floatModeActivatedInfo, currentInfo, generatorFloatTimeMillis
                        )
                    )
                    lastGeneratorNotification = now
                }
            } else {
                cancelGenerator()
            }
        } else {
            // reset the generator notification because the generator is either off or not in float mode
            cancelGenerator()
        }
        return true
    }
    private fun getHostString(host: String?): String {
        if(host != null){
            if(host.length <= 15){ // allow a host like 255.255.255.255 (15 characters)
                return "$host "
            }
            return host.substring(0, 15 - 3) + "... "
        }
        return ""
    }
    private fun getTimedOutSummary(host: String?) = getHostString(host) + "time out ${getTimeString()}"
    private fun getConnectedSummary(host: String?) = getHostString(host) + "success"
    private fun getFailedSummary(host: String?) = getHostString(host) + "fail at ${getTimeString()}"

    private fun setToNoData(dataRequest: DataRequest) {
        val notification = getBuilder()
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setContentText("Connection successful, but no data.")
            .setSubText(getConnectedSummary(dataRequest.host))
            .build()
        notify(notification)
    }
    private fun setToFailedNotification(request: DataRequest){
        if(request.successful){
            throw IllegalArgumentException("Use this method when request.successful == false! It equals true right now!")
        }
        var bigText = ""
        if(request.authDebug != null){
            bigText += request.authDebug + "\n"
        }
        bigText += "Stack trace:\n${request.stackTrace}"

        val notification = getBuilder()
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setContentTitle("Failed to load solar data. Will Try again.")
            .setContentText(request.simpleStatus)
            .setSubText(getFailedSummary(request.host))
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .build()
        notify(notification)
    }
    private fun setToTimedOut(){
        val notification = getBuilder()
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setContentText("Last request timed out. Will try again.")
            .setSubText(getTimedOutSummary(null))
            .build()
        notify(notification)
    }
    private fun setToLoadingNotification(){
        val notification = getBuilder()
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setContentText("Loading Solar Data")
            .setSubText("started loading at ${getTimeString()}")
            .setProgress(2, 1, true)
            .build()
        notify(notification)
    }
    private fun notify(notification: Notification){
        getManager().notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }
    @SuppressWarnings("deprecated")
    private fun getBuilder(): Notification.Builder {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return Notification.Builder(this, NotificationChannels.PERSISTENT_STATUS.id)
        }
        return Notification.Builder(this)
    }
    private fun getManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        println("[123]Stopping persistent service")
        handler.removeCallbacks(this)
        task?.cancel(true) // stop the code from running if it's running
        getManager().cancel(NOTIFICATION_ID)
        cancelGenerator()
    }
    private fun cancelGenerator(){
        getManager().cancel(GENERATOR_NOTIFICATION_ID)
        lastGeneratorNotification = null
    }
    private fun getTimeString() = DateFormat.getTimeInstance().format(Calendar.getInstance().time)
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

