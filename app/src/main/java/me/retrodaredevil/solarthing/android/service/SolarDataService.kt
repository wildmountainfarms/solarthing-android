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
import me.retrodaredevil.solarthing.android.notifications.*
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
        notify(
            getBuilder()
                .loadingNotification()
                .setSmallIcon(R.drawable.solar_panel)
                .build()
        )
    }
    override fun onCancel() {
        service.getManager().cancel(SOLAR_NOTIFICATION_ID)
        cancelGenerator()
    }

    override fun onEnd() {
    }
    override fun onNewDataRequestLoadStart() {
    }

    override fun onTimeout() {
        if(!doNotify(getTimedOutSummary(null))){
            notify(
                getBuilder()
                    .timedOutNotification()
                    .setSmallIcon(R.drawable.solar_panel)
                    .build()
            )
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
                notify(
                    getBuilder()
                        .noDataNotification(dataRequest)
                        .setSmallIcon(R.drawable.solar_panel)
                        .build()
                )
            } else {
                notify(
                    getBuilder()
                        .failedNotification(dataRequest)
                        .setSmallIcon(R.drawable.solar_panel)
                        .build()
                )
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
                    service.getManager().notify(
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
        service.getManager().cancel(GENERATOR_NOTIFICATION_ID)
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


    private fun notify(notification: Notification){
        service.getManager().notify(SOLAR_NOTIFICATION_ID, notification)
    }
    @SuppressWarnings("deprecated")
    private fun getBuilder(): Notification.Builder {
        val builder =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Notification.Builder(service.applicationContext, NotificationChannels.SOLAR_STATUS.id)
        } else {
            Notification.Builder(service.applicationContext)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(getGroup(SOLAR_NOTIFICATION_ID))
        }
        return builder
    }

}