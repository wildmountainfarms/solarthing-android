package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.Service
import android.os.Build
import me.retrodaredevil.solarthing.android.DefaultOptions
import me.retrodaredevil.solarthing.android.Prefs
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarPacketInfo
import me.retrodaredevil.solarthing.android.notifications.*
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.solar.fx.OperationalMode
import me.retrodaredevil.solarthing.solar.mx.ChargerMode
import java.util.*

class SolarDataService(
    private val service: Service,
    private val prefs: Prefs
) : DataService {

    private val packetInfoCollection = TreeSet<SolarPacketInfo>(createComparator { it.dateMillis })
    private var lastFloatGeneratorNotification: Long? = null
    private var lastDoneGeneratorNotification: Long? = null

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
        cancelFloatGeneratorNotification()
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
            packetInfoCollection.limitSize(100_000, 90_000)
            packetInfoCollection.removeIfBefore(System.currentTimeMillis() - 5 * 60 * 60 * 1000) { it.dateMillis }

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
        for (info in packetInfoCollection.reversed()) { // latest packets to oldest
            if (!info.isGeneratorInFloat(virtualFloatModeMinimumBatteryVoltage)) {
                break
            }
            floatModeActivatedInfo = info // get the oldest packet where all the packets up to the current packet have float mode active (the packet where float mode started)
        }
        var doneChargingActivatedInfo: SolarPacketInfo? = null
        for(info in packetInfoCollection.reversed()){ // latest packets to oldest
            if(!(info.generatorOn
                        && info.fxMap.values.none { OperationalMode.CHARGE.isActive(it.operatingMode) || OperationalMode.FLOAT.isActive(it.operatingMode) }
                        )){
                break
            }
            doneChargingActivatedInfo = info
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
                val last = lastFloatGeneratorNotification
                if (last == null || last + DefaultOptions.generatorNotifyIntervalMillis < now) {
                    service.getManager().notify(
                        GENERATOR_FLOAT_NOTIFICATION_ID,
                        NotificationHandler.createFloatGeneratorAlert(
                            service.applicationContext,
                            floatModeActivatedInfo, currentInfo, generatorFloatTimeMillis
                        )
                    )
                    lastFloatGeneratorNotification = now
                }
            } else {
                cancelFloatGeneratorNotification()
            }
        } else {
            // reset the generator notification because the generator is either off or not in float mode
            cancelFloatGeneratorNotification()
        }
        if(doneChargingActivatedInfo != null){
            val now = System.currentTimeMillis()
            val last = lastDoneGeneratorNotification
            if(last == null || last + DefaultOptions.generatorNotifyIntervalMillis < now) {
                service.getManager().notify(
                    GENERATOR_DONE_NOTIFICATION_ID,
                    NotificationHandler.createDoneGeneratorAlert(service.applicationContext, doneChargingActivatedInfo)
                )
                lastDoneGeneratorNotification = now
            }
        } else {
            cancelDoneGeneratorNotification()
        }
        return true
    }
    private fun cancelFloatGeneratorNotification(){
        service.getManager().cancel(GENERATOR_FLOAT_NOTIFICATION_ID)
        lastFloatGeneratorNotification = null
    }
    private fun cancelDoneGeneratorNotification(){
        service.getManager().cancel(GENERATOR_DONE_NOTIFICATION_ID)
        lastDoneGeneratorNotification = null
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