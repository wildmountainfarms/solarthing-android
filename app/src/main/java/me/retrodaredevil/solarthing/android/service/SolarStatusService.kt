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
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.data.*
import me.retrodaredevil.solarthing.android.notifications.*
import me.retrodaredevil.solarthing.android.prefs.DefaultOptions
import me.retrodaredevil.solarthing.android.prefs.MiscProfile
import me.retrodaredevil.solarthing.android.prefs.ProfileProvider
import me.retrodaredevil.solarthing.android.prefs.SolarProfile
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.widget.WidgetHandler
import me.retrodaredevil.solarthing.packets.collection.DefaultInstanceOptions
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.packets.collection.PacketGroups
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.solar.outback.fx.ACMode
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverIdentifier
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import java.util.*
import kotlin.math.max


object SolarPacketCollectionBroadcast {
    const val ACTION = "me.retrodaredevil.solarthing.android.service.SOLAR_PACKET_COLLECTION"
    @Deprecated("Not used anymore")
    const val JSON = "json"
}

class SolarStatusService(
    private val service: Service,
    private val solarProfileProvider: ProfileProvider<SolarProfile>,
    private val miscProfileProvider: ProfileProvider<MiscProfile>,
    private val solarStatusData: PacketGroupData,
    private val solarEventData: PacketGroupData
) : DataService {
    companion object {
        /** This is used when comparing battery voltages in case the battery voltage is something like 26.000001*/
        const val ROUND_OFF_ERROR_DEADZONE = 0.001
//        private val MAPPER = createDefaultObjectMapper()
        private const val MORE_INFO_ACTION = "me.retrodaredevil.solarthing.android.MORE_SOLAR_INFO"
        private const val MORE_INFO_ROVER_ACTION = "me.retrodaredevil.solarthing.android.MORE_ROVER_INFO"
    }

    private val temperatureNotifyHandler = TemperatureNotifyHandler(service, solarProfileProvider, miscProfileProvider)
    private val packetGroups = TreeSet<PacketGroup>(createComparator { it.dateMillis })
    private var solarInfoCollection: Collection<SolarInfo> = emptySet()
    private var lastSolarInfo: SolarInfo? = null
    private var lastVoltageTimerNotification: Long? = null
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

    private lateinit var dataClient: DataClient

    override fun onInit() {
        notify(
            getBuilder()
                .loadingNotification()
                .setSmallIcon(R.drawable.solar_panel)
                .build()
        )
        service.registerReceiver(receiver, IntentFilter(MORE_INFO_ACTION).apply { addAction(MORE_INFO_ROVER_ACTION) })
        dataClient = Wearable.getDataClient(service)
    }
    override fun onCancel() {
        service.getManager().apply {
            cancel(SOLAR_NOTIFICATION_ID)
            cancel(GENERATOR_PERSISTENT_ID)
        }
        cancelVoltageTimerNotification()
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

    private fun getDayStartTimeMillis(dateMillis: Long): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.timeInMillis
    }

    private fun createSolarInfoList(sortedPackets: List<FragmentedPacketGroup>): List<SolarInfo> {
        val previousPackets = mutableListOf<FragmentedPacketGroup>()
        val r = mutableListOf<SolarInfo>()
        for (fragmentedPacketGroup in sortedPackets) {
            val dateMillis = fragmentedPacketGroup.dateMillis
            val solarPacketInfo = try {
                SolarPacketInfo(
                    fragmentedPacketGroup,
                    solarProfileProvider.activeProfile.profile.batteryVoltageType
                )
            } catch (ex: CreationException) {
                ex.printStackTrace()
                println("$dateMillis is a packet collection with something wrong!")
                continue
            }
            val dayStartTimeMillis = getDayStartTimeMillis(dateMillis)
            previousPackets.removeIfBefore(dayStartTimeMillis) { it.dateMillis }
            previousPackets.add(fragmentedPacketGroup)
            val solarDailyInfo = createSolarDailyInfo(dayStartTimeMillis, previousPackets)

            r.add(SolarInfo(solarPacketInfo, solarDailyInfo))
        }
        return r
    }

    override fun onDataRequest(dataRequest: DataRequest) {
        val summary: String

        if(dataRequest.successful) {
            println("[123]Got successful data request")
            val anyAdded = packetGroups.addAll(dataRequest.packetGroupList)
            packetGroups.removeIfBefore(System.currentTimeMillis() - 24 * 60 * 60 * 1000) { it.dateMillis } // remove stuff 24 hours old

            summary = if(anyAdded) getConnectedSummary(dataRequest.host) else getConnectedNoNewDataSummary(dataRequest.host)


            val maxTimeDistance = (miscProfileProvider.activeProfile.profile.maxFragmentTimeMinutes * 60 * 1000).toLong()
            val sortedPackets = PacketGroups.sortPackets(packetGroups, DefaultInstanceOptions.DEFAULT_DEFAULT_INSTANCE_OPTIONS, maxTimeDistance, max(maxTimeDistance, 10 * 60 * 1000))
            if(sortedPackets.isNotEmpty()) {
                solarInfoCollection = createSolarInfoList(sortedPackets.values.first()) // TODO allow multiple instance sources instead of just one
            }

            solarStatusData.onAllPacketReceive(packetGroups.toList())
            if(solarInfoCollection.isNotEmpty()) {
                val intent = Intent(service, WidgetHandler::class.java)
                intent.action = SolarPacketCollectionBroadcast.ACTION
                service.sendBroadcast(intent)

                val solarInfo = solarInfoCollection.last()
                val request = PutDataMapRequest.create("/battery").run {
                    dataMap.putFloat("batteryVoltage", solarInfo.solarPacketInfo.batteryVoltage)
                    asPutDataRequest().setUrgent()
                }
                println(dataClient.api.clientKey)
                println(dataClient.api.name)
                dataClient.putDataItem(request).addOnCompleteListener {
                    println("Completed")
                } // done asynchronously
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
        if(solarInfoCollection.isEmpty()){
            return false
        }
        val currentSolarInfo = solarInfoCollection.last()
        var beginningACDropInfo: SolarPacketInfo? = null
        var lastACDropInfo: SolarPacketInfo? = null
        var acUseInfo: SolarPacketInfo? = null

        var uncertainGeneratorStartInfo = true
        for(solarInfo in solarInfoCollection.reversed()){
            val info = solarInfo.solarPacketInfo
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
//        var voltageTimerActivatedInfo: SolarPacketInfo? = null
        val activeSolarProfile = solarProfileProvider.activeProfile.profile
//        val voltageTimerBatteryVoltage = activeSolarProfile.voltageTimerBatteryVoltage
//        if(voltageTimerBatteryVoltage != null && currentInfo.acMode == ACMode.AC_USE){ // TODO make this happen in any AC mode and remove from generator notification
//            for (info in packetInfoCollection.reversed()) { // latest packets to oldest
//                if (!info.isBatteryVoltageAboveSetpoint(voltageTimerBatteryVoltage)) {
//                    break
//                }
//                voltageTimerActivatedInfo = info
//            }
//        }
        var doneChargingActivatedInfo: SolarPacketInfo? = null
        for(solarInfo in solarInfoCollection.reversed()){ // latest packets to oldest
            val info = solarInfo.solarPacketInfo
            if(info.acMode != ACMode.AC_USE || info.generatorChargingBatteries){
                break
            }
            doneChargingActivatedInfo = info
        }

        notify(NotificationHandler.createStatusNotification(
            service.applicationContext,
            currentSolarInfo.solarPacketInfo,
            currentSolarInfo.solarDailyInfo,
            summary,
            moreInfoIntent,
            miscProfileProvider.activeProfile.profile.temperatureUnit
        ))
//        service.getManager().notify(
//            END_OF_DAY_NOTIFICATION_ID,
//            NotificationHandler.createDayEnd(service.applicationContext, currentSolarInfo.solarPacketInfo, currentSolarInfo.solarDailyInfo)
//        )
        if(beginningACDropInfo != null || acUseInfo != null){
            service.getManager().notify(
                GENERATOR_PERSISTENT_ID,
                NotificationHandler.createPersistentGenerator(
                    service, currentSolarInfo.solarPacketInfo,
                    beginningACDropInfo, lastACDropInfo, acUseInfo,
                    uncertainGeneratorStartInfo
                )
            )
        } else {
            service.getManager().cancel(GENERATOR_PERSISTENT_ID)
        }
        val lastSolarInfo = this.lastSolarInfo
        this.lastSolarInfo = currentSolarInfo
        if(lastSolarInfo != null){
            if (currentSolarInfo.solarDailyInfo.dayStartTimeMillis - lastSolarInfo.solarDailyInfo.dayStartTimeMillis > 2 * 60 * 60 * 1000) {
                println("End of day ${lastSolarInfo.solarDailyInfo.dayStartTimeMillis} new: ${currentSolarInfo.solarDailyInfo.dayStartTimeMillis}")
                service.getManager().notify(
                    END_OF_DAY_NOTIFICATION_ID,
                    NotificationHandler.createDayEnd(service, lastSolarInfo.solarPacketInfo, lastSolarInfo.solarDailyInfo)
                )
            }
            // region Device Connection/Disconnection section
            for((identifierFragment, device) in currentSolarInfo.solarPacketInfo.deviceMap.entries){
                val presentInLast = identifierFragment in lastSolarInfo.solarPacketInfo.deviceMap
                if(!presentInLast){ // device just connected
                    val notificationAndSummary = NotificationHandler.createDeviceConnectionStatus(service, device, true, currentSolarInfo.solarPacketInfo.dateMillis)
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
            for((identifierFragment, device) in lastSolarInfo.solarPacketInfo.deviceMap.entries){
                val presentNow = identifierFragment in currentSolarInfo.solarPacketInfo.deviceMap
                if(!presentNow){ // device just disconnected
                    val notificationAndSummary = NotificationHandler.createDeviceConnectionStatus(service, device, false, currentSolarInfo.solarPacketInfo.dateMillis)
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


        // region Voltage Timer notification
        /* TODO implement multi voltage timers
        if(voltageTimerActivatedInfo != null){
            // check to see if we should send a notification
            val voltageTimerTimeMillis = (activeSolarProfile.voltageTimerTimeHours * 60 * 60 * 1000).toLong()
            val now = System.currentTimeMillis()
            if(voltageTimerActivatedInfo.dateMillis + voltageTimerTimeMillis < now) { // should it be turned off?
                val last = lastVoltageTimerNotification
                if (last == null || last + DefaultOptions.importantAlertIntervalMillis < now) {
                    service.getManager().notify(
                        VOLTAGE_TIMER_NOTIFICATION_ID,
                        NotificationHandler.createVoltageTimerAlert(
                            service.applicationContext,
                            voltageTimerActivatedInfo, currentSolarInfo.solarPacketInfo, voltageTimerTimeMillis
                        )
                    )
                    lastVoltageTimerNotification = now
                }
            } else {
                cancelVoltageTimerNotification()
            }
        } else {
            // reset the generator notification because the generator is either off or not in float mode
            cancelVoltageTimerNotification()
        }
         */
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
        if(criticalBatteryVoltage != null && currentSolarInfo.solarPacketInfo.batteryVoltage <= criticalBatteryVoltage + ROUND_OFF_ERROR_DEADZONE){ // critical alert
            val now = System.currentTimeMillis()
            val last = lastCriticalBatteryNotification
            if(last == null || last + DefaultOptions.importantAlertIntervalMillis < now) {
                service.getManager().notify(
                    BATTERY_NOTIFICATION_ID,
                    NotificationHandler.createBatteryNotification(service, currentSolarInfo.solarPacketInfo, true)
                )
                lastCriticalBatteryNotification = now
                lastLowBatteryNotification = now
            }
        } else if(lowBatteryVoltage != null && currentSolarInfo.solarPacketInfo.batteryVoltage <= lowBatteryVoltage + ROUND_OFF_ERROR_DEADZONE){ // low alert
            val now = System.currentTimeMillis()
            val last = lastLowBatteryNotification
            if(last == null || last + DefaultOptions.importantAlertIntervalMillis < now) {
                service.getManager().notify(
                    BATTERY_NOTIFICATION_ID,
                    NotificationHandler.createBatteryNotification(service, currentSolarInfo.solarPacketInfo, false)
                )
                lastLowBatteryNotification = now
            }
        }
        // endregion
        // region Temperature Alert Notifications
        for(rover in currentSolarInfo.solarPacketInfo.roverMap.values){
            val batteryTemperature = rover.batteryTemperatureCelsius
            val controllerTemperature = rover.controllerTemperatureCelsius
            temperatureNotifyHandler.checkBatteryTemperature(currentSolarInfo.solarPacketInfo.dateMillis, rover, batteryTemperature.toFloat())
            temperatureNotifyHandler.checkControllerTemperature(currentSolarInfo.solarPacketInfo.dateMillis, rover, controllerTemperature.toFloat())
        }
        for((fragmentId, temperatureCelsius) in currentSolarInfo.solarPacketInfo.deviceCpuTemperatureMap) {
            temperatureNotifyHandler.checkDeviceCpuTemperature(currentSolarInfo.solarPacketInfo.dateMillis, fragmentId, temperatureCelsius)
        }
        // endregion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifyMoreInfoUpdatePresent(currentSolarInfo.solarPacketInfo)
        }
        return true
    }
    private fun cancelVoltageTimerNotification(){
        service.getManager().cancel(VOLTAGE_TIMER_NOTIFICATION_ID)
        lastVoltageTimerNotification = null
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
                    val packetInfo = lastSolarInfo?.solarPacketInfo ?: run { System.err.println("lastSolarInfo is null when more info is requested!"); return }
                    notifyMoreInfo(packetInfo)
                }
                MORE_INFO_ROVER_ACTION -> {
                    var fragmentId: Int? = intent.getIntExtra("fragment", -1)
                    if(fragmentId == -1){
                        if (intent.getIntExtra("fragment", -2) == -2) {
                            fragmentId = null
                        }
                    }
                    val packetInfo = lastSolarInfo?.solarPacketInfo ?: run { System.err.println("lastSolarInfo is null when more info is requested!"); return }
                    val identifierFragment = IdentifierFragment.create(fragmentId, RoverIdentifier.getDefaultIdentifier())
                    val rover = packetInfo.roverMap[identifierFragment] ?: run { System.err.println("no rover with fragment $fragmentId"); return}
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

        for(device in getOrderedValues(
            packetInfo.deviceMap
        )){
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
        get() = if(solarInfoCollection.isEmpty())
            UpdatePeriodType.LARGE_DATA
        else
            UpdatePeriodType.SMALL_DATA

    override val startKey: Long
        get() = if(solarInfoCollection.isEmpty()) {
            getDayStartTimeMillis(System.currentTimeMillis())
        } else
            solarInfoCollection.last().solarPacketInfo.dateMillis

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
