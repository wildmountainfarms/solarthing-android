package me.retrodaredevil.solarthing.android.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import me.retrodaredevil.iot.outhouse.OuthousePackets
import me.retrodaredevil.iot.packets.PacketCollections
import me.retrodaredevil.iot.solar.SolarPackets
import me.retrodaredevil.solarthing.android.Prefs
import me.retrodaredevil.solarthing.android.clone
import me.retrodaredevil.solarthing.android.request.CouchDbDataRequester
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.request.DataRequester
import me.retrodaredevil.solarthing.android.request.DataRequesterMultiplexer


fun restartService(context: Context){
    val serviceIntent = Intent("me.retrodaredevil.solarthing.android.service.PersistentService")
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

private class ServiceObject(
    val dataService: DataService,
    val databaseName: String,
    val jsonPacketGetter: PacketCollections.JsonPacketGetter
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
        ServiceObject(SolarDataService(this, prefs), "solarthing",
            PacketCollections.JsonPacketGetter { packetObject -> SolarPackets.createFromJson(packetObject) }),
        ServiceObject(OuthouseDataService(this), "outhouse",
            PacketCollections.JsonPacketGetter { packetObject -> OuthousePackets.createFromJson(packetObject) })
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
        Toast.makeText(this, "SolarThing Notification Service Started", Toast.LENGTH_LONG).show()
        println("Starting service")
        return START_STICKY
    }

    override fun run() {
        var needsLargeData = false
        for(service in services){
            val task = service.task
            if(task != null){
                if(task.cancel(true)) { // if the task was still running then...
                    service.dataService.onTimeout()
                }
            }

            service.dataRequesters = prefs.createCouchDbProperties().map{
                CouchDbDataRequester(
                    {it.clone().apply { dbName = service.databaseName }},
                    service.jsonPacketGetter,
                    { service.dataService.startKey }
                )
            }
            if(service.dataService.updatePeriodType == UpdatePeriodType.LARGE_DATA){
                needsLargeData = true
            }
            service.task = DataUpdaterTask(service.dataRequester, service.dataService::onDataRequest).execute()
        }

        if(needsLargeData){
            handler.postDelayed(this, prefs.initialRequestTimeSeconds * 1000L)
        } else {
            handler.postDelayed(this, prefs.subsequentRequestTimeSeconds * 1000L)
        }
    }

    override fun onDestroy() {
        println("[123]Stopping persistent service")
        handler.removeCallbacks(this)
        for(service in services){
            service.task?.cancel(true)
            service.dataService.onEnd()
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

