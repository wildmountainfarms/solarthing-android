package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import me.retrodaredevil.solarthing.android.DefaultOptions
import me.retrodaredevil.solarthing.android.Prefs
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarPacketInfo
import me.retrodaredevil.solarthing.android.notifications.GENERATOR_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.notifications.SOLAR_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.request.DataRequest
import java.util.*
import kotlin.Comparator

class SolarDataService(
    private val service: Service,
    private val prefs: Prefs
) : DataService {

    private val packetInfoCollection: MutableCollection<SolarPacketInfo> = TreeSet(Comparator { o1, o2 -> (o1.dateMillis - o2.dateMillis).toInt() })
    private var lastGeneratorNotification: Long? = null

    override fun onInit() {
        setToLoadingNotification()
    }
    override fun onCancel() {
        getManager().cancel(SOLAR_NOTIFICATION_ID)
        cancelGenerator()
    }
    override fun onNewDataRequestLoadStart() {
    }

    override fun onTimeout() {
        if(!doNotify(getTimedOutSummary(null))){
            setToTimedOut()
        }
    }

    override fun onDataRequest(dataRequest: DataRequest) {
        val summary: String

        if(dataRequest.successful) {
            println("[123]Got successful data request")
            packetInfoCollection.addAll(dataRequest.packetCollectionList.map {
                SolarPacketInfo(
                    it
                )
            })
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
        var floatModeActivatedInfo: SolarPacketInfo? = null
        val virtualFloatModeMinimumBatteryVoltage = prefs.virtualFloatModeMinimumBatteryVoltage
        for(info in packetInfoCollection.reversed()){ // go through latest packets first
            if(!info.isGeneratorInFloat(virtualFloatModeMinimumBatteryVoltage)){
                break
            }
            floatModeActivatedInfo = info // get the oldest packet where all the packets up to the current packet have float mode active (the packet where float mode started)
        }
        val notification = NotificationHandler.createStatusNotification(
            service.applicationContext,
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
                            service.applicationContext,
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
    private fun cancelGenerator(){
        getManager().cancel(GENERATOR_NOTIFICATION_ID)
        lastGeneratorNotification = null
    }

    override val updatePeriodType: UpdatePeriodType
        get() = if(packetInfoCollection.isEmpty())
            UpdatePeriodType.LARGE_DATA
        else
            UpdatePeriodType.SMALL_DATA

    override val startKey: Long
        get() = if(packetInfoCollection.isEmpty())
            System.currentTimeMillis() - 2 * 60 * 60 * 1000 // 2 hours in the past
        else
            packetInfoCollection.last().dateMillis

    override val shouldUpdate: Boolean
        get() = NotificationChannels.SOLAR_STATUS.isCurrentlyEnabled(service)


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
        getManager().notify(SOLAR_NOTIFICATION_ID, notification)
    }
    @SuppressWarnings("deprecated")
    private fun getBuilder(): Notification.Builder {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return Notification.Builder(service.applicationContext, NotificationChannels.SOLAR_STATUS.id)
        }
        return Notification.Builder(service.applicationContext)
    }
    private fun getManager() = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

}