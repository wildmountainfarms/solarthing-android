package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import me.retrodaredevil.solarthing.android.PacketGroup
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.OUTHOUSE_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.VACANT_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.getGroup
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.outhouse.*
import me.retrodaredevil.solarthing.packets.Modes
import java.util.*
import kotlin.math.round


class OuthouseDataService(
    private val service: Service
) : DataService {
    companion object {
        private const val VACANT_NOTIFY_ACTION = "me.retrodaredevil.solarthing.android.service.action.enable_vacant_notify"
        private const val VACANT_NOTIFY_CLEAR_ACTION = "me.retrodaredevil.solarthing.android.service.action.disable_vacant_notify"
    }

    private val packetCollections = TreeSet<PacketGroup>(createComparator { it.dateMillis })

    private var vacantNotify = false

    override fun onInit() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(VACANT_NOTIFY_ACTION)
        service.registerReceiver(vacantNotifyReceiver, intentFilter)

        val clearIntentFilter = IntentFilter()
        clearIntentFilter.addAction(VACANT_NOTIFY_CLEAR_ACTION)
        service.registerReceiver(vacantNotifyClearReceiver, clearIntentFilter)

        service.getManager().notify(
            OUTHOUSE_NOTIFICATION_ID,
            getBuilder(NotificationChannels.OUTHOUSE_STATUS_WHILE_VACANT)
                .loadingNotification()
                .setSmallIcon(R.drawable.potty)
                .build()
        )
    }

    override fun onCancel() {
        service.getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
    }

    override fun onEnd() {
        service.unregisterReceiver(vacantNotifyReceiver)
        service.unregisterReceiver(vacantNotifyClearReceiver)
    }

    override fun onNewDataRequestLoadStart() {
    }

    override fun onDataRequest(dataRequest: DataRequest) {
        if(dataRequest.successful){
            packetCollections.addAll(dataRequest.packetGroupList)
            packetCollections.limitSize(100_000, 90_000)
            packetCollections.removeIfBefore(System.currentTimeMillis() - 20 * 60 * 1000) { it.dateMillis } // keep packets 20 minutes in the past
            if(!doNotify(getConnectedSummary(dataRequest.host))){
                service.getManager().notify(
                    OUTHOUSE_NOTIFICATION_ID,
                    getBuilder(NotificationChannels.OUTHOUSE_STATUS_WHILE_VACANT)
                        .noDataNotification(dataRequest)
                        .setSmallIcon(R.drawable.potty)
                        .build()
                )
            }
        } else {
            println("unsuccessful outhouse data request")
            if(!doNotify(getFailedSummary(dataRequest.host))){
                service.getManager().notify(
                    OUTHOUSE_NOTIFICATION_ID,
                    getBuilder(NotificationChannels.OUTHOUSE_STATUS_WHILE_VACANT)
                        .failedNotification(dataRequest)
                        .setSmallIcon(R.drawable.potty)
                        .build()
                )
            }
        }
    }
    private fun getTemperatureString(temperatureCelsius: Number): String {
        val f = temperatureCelsius.toDouble() * 9 / 5 + 32
        return "$f (F)"
    }

    override fun onTimeout() {
        if(!doNotify(getTimedOutSummary(null))){
            service.getManager().notify(
                OUTHOUSE_NOTIFICATION_ID,
                getBuilder(NotificationChannels.OUTHOUSE_STATUS_WHILE_VACANT)
                    .timedOutNotification()
                    .setSmallIcon(R.drawable.potty)
                    .build()
            )
        }
    }

    private fun doNotify(summary: String): Boolean{
        if(packetCollections.isEmpty()){
            return false
        }
        val packetCollection = packetCollections.last() // most recent

        var occupancyPacket: OccupancyPacket? = null
        var weatherPacket: WeatherPacket? = null
        for(packet in packetCollection.packets){
            if(packet is OuthousePacket){
                when(packet.packetType){
                    OuthousePacketType.OCCUPANCY -> occupancyPacket = packet as OccupancyPacket
                    OuthousePacketType.WEATHER -> weatherPacket = packet as WeatherPacket
                    null -> throw NullPointerException()
                    else -> {}
                }
            }
        }
        val occupancy: Occupancy? = if(occupancyPacket != null) Modes.getActiveMode(Occupancy::class.java, occupancyPacket.occupancy) else null
        if(occupancy == Occupancy.OCCUPIED && !vacantNotify){
            service.getManager().cancel(VACANT_NOTIFICATION_ID)
        }
        if(occupancy == Occupancy.VACANT && vacantNotify){
            service.getManager().notify(VACANT_NOTIFICATION_ID,
                getBuilder(NotificationChannels.VACANT_NOTIFICATION)
                    .setSmallIcon(R.drawable.potty)
                    .setContentTitle("Outhouse is now vacant!")
                    .setShowWhen(true)
                    .setWhen(packetCollection.dateMillis)
                    .build()
            )
            vacantNotify = false
        }
        val builder = getBuilder(if(occupancy == Occupancy.OCCUPIED) NotificationChannels.OUTHOUSE_STATUS_WHILE_OCCUPIED else NotificationChannels.OUTHOUSE_STATUS_WHILE_VACANT)
            .setSmallIcon(if(occupancy == Occupancy.OCCUPIED) R.drawable.potty_occupied else R.drawable.potty)
            .setContentTitle("Outhouse: " + if(occupancy != null) occupancy.modeName else "NO OCCUPANCY DATA")
            .setContentText(if(weatherPacket == null) "no weather data" else "Temperature: ${getTemperatureString(weatherPacket.temperatureCelsius)} " +
                    "| Humidity: ${round(weatherPacket.humidityPercent.toDouble()).toInt()}%")
            .setSubText(summary)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(packetCollection.dateMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//            builder.setSortKey("d")
            builder.setGroup(getGroup(OUTHOUSE_NOTIFICATION_ID))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(occupancy == Occupancy.OCCUPIED){
                builder.setColor(Color.RED)
            } else {
                builder.setColor(Color.GREEN)
            }
            builder.setCategory(Notification.CATEGORY_STATUS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(occupancy == Occupancy.OCCUPIED) {
                val pendingIntent = PendingIntent.getBroadcast(
                    service, 0,
                    Intent(VACANT_NOTIFY_ACTION),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                builder.addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(service, R.drawable.potty),
                        "Enable vacant notify",
                        pendingIntent
                    ).build()
                )
            }
        }
        service.getManager().notify(OUTHOUSE_NOTIFICATION_ID, builder.build())
        return true
    }

    override val updatePeriodType = UpdatePeriodType.SMALL_DATA
    override val startKey: Long
        get() = if(packetCollections.isEmpty())
            System.currentTimeMillis() - 5 * 60 * 1000
        else
            packetCollections.last().dateMillis

    override val shouldUpdate: Boolean
        get() = NotificationChannels.OUTHOUSE_STATUS_WHILE_VACANT.isCurrentlyEnabled(service) || NotificationChannels.OUTHOUSE_STATUS_WHILE_OCCUPIED.isCurrentlyEnabled(service)

    @SuppressWarnings("deprecated")
    private fun getBuilder(notificationChannels: NotificationChannels): Notification.Builder {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return Notification.Builder(service.applicationContext, notificationChannels.id)
        }
        return Notification.Builder(service.applicationContext)
    }

    private val vacantNotifyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            println("vacantNotifyReceiver")
            vacantNotify = true
            println("set vacantNotify to true!")
            val builder = getBuilder(NotificationChannels.SILENT_VACANT_NOTIFICATION)
                    .setSmallIcon(R.drawable.potty_occupied)
                    .setContentTitle("You will be notified when outhouse is vacant")
                    .setContentText("Clear notification to disable notification")
                    .setDeleteIntent(
                        PendingIntent.getBroadcast(
                            service,
                            0,
                            Intent(VACANT_NOTIFY_CLEAR_ACTION),
                            PendingIntent.FLAG_CANCEL_CURRENT
                        )
                    )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//                builder.setSortKey("b")
                builder.setGroup(getGroup(VACANT_NOTIFICATION_ID))
            }
            service.getManager().notify(VACANT_NOTIFICATION_ID, builder.build())
        }
    }
    private val vacantNotifyClearReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            vacantNotify = false
            println("set vacantNotify to false!")
        }

    }
}
