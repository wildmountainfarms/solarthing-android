package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarPacketInfo
import me.retrodaredevil.solarthing.android.WidgetHandler
import me.retrodaredevil.solarthing.android.getOrderedValues
import me.retrodaredevil.solarthing.android.notifications.*
import me.retrodaredevil.solarthing.android.prefs.DefaultOptions
import me.retrodaredevil.solarthing.android.prefs.MiscProfile
import me.retrodaredevil.solarthing.android.prefs.ProfileProvider
import me.retrodaredevil.solarthing.android.prefs.SolarProfile
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.packets.collection.PacketGroups
import me.retrodaredevil.solarthing.solar.outback.fx.ACMode
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverIdentifier
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import me.retrodaredevil.solarthing.util.JacksonUtil
import java.util.*
import kotlin.math.max

object SolarPacketCollectionBroadcast {
    const val ACTION = "me.retrodaredevil.solarthing.android.service.SOLAR_PACKET_COLLECTION"
    const val JSON = "json"
}

class SolarStatusService(
    private val service: Service,
    private val solarProfileProvider: ProfileProvider<SolarProfile>,
    private val miscProfileProvider: ProfileProvider<MiscProfile>,
    private val solarEventData: SolarEventData
) : DataService {
    companion object {
        /** This is used when comparing battery voltages in case the battery voltage is something like 26.000001*/
        const val ROUND_OFF_ERROR_DEADZONE = 0.001
        private val MAPPER = JacksonUtil.defaultMapper()
        private const val MORE_INFO_ACTION = "me.retrodaredevil.solarthing.android.MORE_SOLAR_INFO"
        private const val MORE_INFO_ROVER_ACTION = "me.retrodaredevil.solarthing.android.MORE_ROVER_INFO"
    }

    private val packetGroups = TreeSet<PacketGroup>(createComparator { it.dateMillis })
    private var packetInfoCollection: Collection<SolarPacketInfo> = emptySet()
    private var lastPacketInfo: SolarPacketInfo? = null
    private var lastFloatGeneratorNotification: Long? = null
    private var lastDoneGeneratorNotification: Long? = null
    private var lastLowBatteryNotification: Long? = null
    private var lastCriticalBatteryNotification: Long? = null

    private val moreInfoIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            service,
            0,
            Intent(MORE_INFO_ACTION),
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    override fun onInit() {
        notify(
            getBuilder()
                .loadingNotification()
                .setSmallIcon(R.drawable.solar_panel)
                .build()
        )
        service.registerReceiver(receiver, IntentFilter(MORE_INFO_ACTION).apply { addAction(MORE_INFO_ROVER_ACTION) })
    }
    override fun onCancel() {
        service.getManager().apply {
            cancel(SOLAR_NOTIFICATION_ID)
            cancel(GENERATOR_PERSISTENT_ID)
        }
        cancelFloatGeneratorNotification()
    }

    override fun onEnd() {
        service.unregisterReceiver(receiver)
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
            val anyAdded = packetGroups.addAll(dataRequest.packetGroupList)
            packetGroups.limitSize(100_000, 90_000)
            packetGroups.removeIfBefore(System.currentTimeMillis() - 11 * 60 * 60 * 1000) { it.dateMillis } // remove stuff 11 hours old

            summary = if(anyAdded) getConnectedSummary(dataRequest.host) else getConnectedNoNewDataSummary(dataRequest.host)


            val maxTimeDistance = (miscProfileProvider.activeProfile.maxFragmentTimeMinutes * 60 * 1000).toLong()
            val sortedPackets = PacketGroups.sortPackets(packetGroups, maxTimeDistance, max(maxTimeDistance, 10 * 60 * 1000))
            if(sortedPackets.isNotEmpty()) {
                packetInfoCollection = sortedPackets.values.first().mapNotNull {// TODO allow multiple instance sources instead of just one
                    try {
                        SolarPacketInfo(it, solarProfileProvider.activeProfile.batteryVoltageType)
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                        println("${it.dateMillis} is a packet collection without packets")
                        null
                    }
                }
            }


            if(packetInfoCollection.isNotEmpty()) {
                val intent = Intent(service, WidgetHandler::class.java)
                intent.action = SolarPacketCollectionBroadcast.ACTION
                val latest = packetInfoCollection.last()
                intent.putExtra(SolarPacketCollectionBroadcast.JSON, MAPPER.writeValueAsString(latest.packetGroup))
                service.sendBroadcast(intent)
            }
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
        var beginningACDropInfo: SolarPacketInfo? = null
        var lastACDropInfo: SolarPacketInfo? = null
        var acUseInfo: SolarPacketInfo? = null

        var uncertainGeneratorStartInfo = true
        for(info in packetInfoCollection.reversed()){
            if(info.acMode == ACMode.NO_AC){
                uncertainGeneratorStartInfo = false
                break
            }
            when(info.acMode){
                ACMode.AC_USE -> acUseInfo = info
                ACMode.AC_DROP -> {
                    lastACDropInfo = info
                    beginningACDropInfo = info
                }
                else -> {}
            }
        }
        if(beginningACDropInfo != null && acUseInfo != null && beginningACDropInfo.dateMillis > acUseInfo.dateMillis){
            beginningACDropInfo = null // beginningACDropInfo didn't actually happen before AC Use started so set it to null
        }
        var floatModeActivatedInfo: SolarPacketInfo? = null
        val activeSolarProfile = solarProfileProvider.activeProfile
        val virtualFloatModeMinimumBatteryVoltage = activeSolarProfile.virtualFloatMinimumBatteryVoltage
        for (info in packetInfoCollection.reversed()) { // latest packets to oldest
            if (!info.isGeneratorInFloat(virtualFloatModeMinimumBatteryVoltage)) {
                break
            }
            floatModeActivatedInfo = info // get the oldest packet where all the packets up to the current packet have float mode active (the packet where float mode started)
        }
        var doneChargingActivatedInfo: SolarPacketInfo? = null
        for(info in packetInfoCollection.reversed()){ // latest packets to oldest
            if(info.acMode != ACMode.AC_USE || info.generatorChargingBatteries){
                break
            }
            doneChargingActivatedInfo = info
        }

        notify(
            NotificationHandler.createStatusNotification(
                service.applicationContext,
                currentInfo,
                summary,
                moreInfoIntent
            )
        )
        if(beginningACDropInfo != null || acUseInfo != null){
            service.getManager().notify(
                GENERATOR_PERSISTENT_ID,
                NotificationHandler.createPersistentGenerator(
                    service, currentInfo,
                    beginningACDropInfo, lastACDropInfo, acUseInfo,
                    floatModeActivatedInfo,
                    (activeSolarProfile.generatorFloatTimeHours * 60 * 60 * 1000).toLong(), uncertainGeneratorStartInfo
                )
            )
        } else {
            service.getManager().cancel(GENERATOR_PERSISTENT_ID)
        }
        val lastPacketInfo = this.lastPacketInfo
        if(lastPacketInfo != null){
            // end of day for loop
            /*
            for(dailyChargeController in currentInfo.dailyChargeControllerMap.values){
                if(dailyChargeController !is Identifiable) error("dailyChargeControllerMap must be identifiable!")
                if(dailyChargeController.dailyKWH == 0f){
                    val lastDailyChargeController = lastPacketInfo.dailyChargeControllerMap[dailyChargeController.identifier] ?: continue
                    val dailyKWH = lastDailyChargeController.dailyKWH
                    if(dailyKWH != 0f){
                        val notificationAndSummary = NotificationHandler.createEndOfDay(service, currentInfo, lastDailyChargeController, currentInfo.dateMillis)
                        service.getManager().notify(
                            getEndOfDayInfoId(lastDailyChargeController),
                            notificationAndSummary.first
                        )
                        service.getManager().notify(
                            END_OF_DAY_SUMMARY_ID,
                            notificationAndSummary.second
                        )
                    }
                }
            }
             */
            // region Device Connection/Disconnection section
            for(device in currentInfo.deviceMap.values){
                val presentInLast = device.identifier in lastPacketInfo.deviceMap
                if(!presentInLast){ // device just connected
                    val notificationAndSummary = NotificationHandler.createDeviceConnectionStatus(service, device, true, currentInfo.dateMillis)
                    service.getManager().apply {
                        notify(
                            getDeviceConnectionStatusId(device),
                            notificationAndSummary.first
                        )
                        notify(
                            DEVICE_CONNECTION_STATUS_SUMMARY_ID,
                            notificationAndSummary.second
                        )
                    }
                }
            }
            for(device in lastPacketInfo.deviceMap.values){
                val presentNow = device.identifier in currentInfo.deviceMap
                if(!presentNow){ // device just disconnected
                    val notificationAndSummary = NotificationHandler.createDeviceConnectionStatus(service, device, false, currentInfo.dateMillis)
                    service.getManager().apply {
                        notify(
                            getDeviceConnectionStatusId(device),
                            notificationAndSummary.first
                        )
                        notify(
                            DEVICE_CONNECTION_STATUS_SUMMARY_ID,
                            notificationAndSummary.second
                        )
                    }
                }
            }
            // endregion
        }
        this.lastPacketInfo = currentInfo


        // region Generator float timer notification
        if(floatModeActivatedInfo != null){
            // check to see if we should send a notification
            val generatorFloatTimeMillis = (activeSolarProfile.generatorFloatTimeHours * 60 * 60 * 1000).toLong()
            val now = System.currentTimeMillis()
            if(floatModeActivatedInfo.dateMillis + generatorFloatTimeMillis < now) { // should it be turned off?
                val last = lastFloatGeneratorNotification
                if (last == null || last + DefaultOptions.importantAlertIntervalMillis < now) {
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
        // endregion

        // region Generator Done Charging Notification
        if(doneChargingActivatedInfo != null){
            val now = System.currentTimeMillis()
            val last = lastDoneGeneratorNotification
            if(last == null || last + DefaultOptions.importantAlertIntervalMillis < now) {
                service.getManager().notify(
                    GENERATOR_DONE_NOTIFICATION_ID,
                    NotificationHandler.createDoneGeneratorAlert(service.applicationContext, doneChargingActivatedInfo)
                )
                lastDoneGeneratorNotification = now
            }
        } else {
            cancelDoneGeneratorNotification()
        }
        // endregion

        // region Battery Alert Notifications
        val criticalBatteryVoltage = activeSolarProfile.criticalBatteryVoltage
        val lowBatteryVoltage = activeSolarProfile.lowBatteryVoltage
        if(criticalBatteryVoltage != null && currentInfo.batteryVoltage <= criticalBatteryVoltage + ROUND_OFF_ERROR_DEADZONE){ // critical alert
            val now = System.currentTimeMillis()
            val last = lastCriticalBatteryNotification
            if(last == null || last + DefaultOptions.importantAlertIntervalMillis < now) {
                service.getManager().notify(
                    BATTERY_NOTIFICATION_ID,
                    NotificationHandler.createBatteryNotification(service, currentInfo, true)
                )
                lastCriticalBatteryNotification = now
                lastLowBatteryNotification = now
            }
        } else if(lowBatteryVoltage != null && currentInfo.batteryVoltage <= lowBatteryVoltage + ROUND_OFF_ERROR_DEADZONE){ // low alert
            val now = System.currentTimeMillis()
            val last = lastLowBatteryNotification
            if(last == null || last + DefaultOptions.importantAlertIntervalMillis < now) {
                service.getManager().notify(
                    BATTERY_NOTIFICATION_ID,
                    NotificationHandler.createBatteryNotification(service, currentInfo, false)
                )
                lastLowBatteryNotification = now
            }
        }
        // endregion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifyMoreInfoUpdatePresent(currentInfo)
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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context!!; intent!!
            when(intent.action){
                MORE_INFO_ACTION -> {
                    val packetInfo = lastPacketInfo ?: run { System.err.println("lastPacketInfo is null when more info is requested!"); return }
                    notifyMoreInfo(packetInfo)
                }
                MORE_INFO_ROVER_ACTION -> {
                    val serial = intent.getIntExtra("rover_serial", -1)
                    if(serial == -1){
                        System.err.println("serial is -1!")
                        return
                    }
                    val packetInfo = lastPacketInfo ?: run { System.err.println("lastPacketInfo is null when more info is requested!"); return }
//                    val rover = packetInfo.roverMap.values.firstOrNull { it.productSerialNumber == serial } ?: run { System.err.println("no rover with serial $serial"); return}
                    val rover = packetInfo.roverMap[RoverIdentifier(serial)] ?: run { System.err.println("no rover with serial $serial"); return}
                    notifyMoreRoverInfo(packetInfo, rover)
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun notifyMoreInfoUpdatePresent(packetInfo: SolarPacketInfo){
        notifyMoreInfo(packetInfo, service.getManager().activeNotifications)
    }
    private fun notifyMoreInfo(packetInfo: SolarPacketInfo, statusBarNotifications: Array<StatusBarNotification>? = null){
        val manager = service.getManager()
        var summary: Notification? = null

        for(device in getOrderedValues(packetInfo.deviceMap)){
            val id = getMoreSolarInfoId(device)
            if(statusBarNotifications == null || statusBarNotifications.any { it.id == id }) {
                val dateMillis = packetInfo.packetGroup.getDateMillis(device) ?: packetInfo.dateMillis
                val pair = NotificationHandler.createMoreInfoNotification(service, device, dateMillis, packetInfo, MORE_INFO_ROVER_ACTION)
                val notification = pair.first
                summary = pair.second
                manager.notify(id, notification)
            }
            if(device is RoverStatusPacket) {
                if (statusBarNotifications != null && statusBarNotifications.any { it.id == id + 1 }) {
                    notifyMoreRoverInfo(packetInfo, device)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if(summary != null){
                manager.notify(MORE_SOLAR_INFO_SUMMARY_ID, summary)
            }
        }
    }
    private fun notifyMoreRoverInfo(packetInfo: SolarPacketInfo, rover: RoverStatusPacket){
        val dateMillis = packetInfo.packetGroup.getDateMillis(rover) ?: packetInfo.dateMillis
        val pair = NotificationHandler.createMoreRoverInfoNotification(service, rover, dateMillis)

        val id = getMoreSolarInfoId(rover) + 1
        service.getManager().apply {
            notify(id, pair.first)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                if (pair.second != null) {
                    notify(MORE_SOLAR_INFO_SUMMARY_ID, pair.second)
                }
            }
        }
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
        val builder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Notification.Builder(service, NotificationChannels.SOLAR_STATUS.id)
        } else {
            Notification.Builder(service)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(getGroup(SOLAR_NOTIFICATION_ID))
        }
        return builder
    }

}
