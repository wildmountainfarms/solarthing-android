package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import me.retrodaredevil.solarthing.outhouse.*
import me.retrodaredevil.solarthing.packets.Modes
import me.retrodaredevil.solarthing.packets.collection.PacketCollection
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.OUTHOUSE_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.VACANT_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.getGroup
import me.retrodaredevil.solarthing.android.request.DataRequest
import java.util.*
import kotlin.Comparator
import kotlin.math.round


class OuthouseDataService(
    private val service: Service
) : DataService {
    companion object {
        private const val VACANT_NOTIFY_ACTION = "me.retrodaredevil.solarthing.android.service.action.enable_vacant_notify"
        private const val VACANT_NOTIFY_CLEAR_ACTION = "me.retrodaredevil.solarthing.android.service.action.disable_vacant_notify"
    }

    private var vacantNotify = false

    override fun onInit() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(VACANT_NOTIFY_ACTION)
        service.registerReceiver(vacantNotifyReceiver, intentFilter)

        val clearIntentFilter = IntentFilter()
        clearIntentFilter.addAction(VACANT_NOTIFY_CLEAR_ACTION)
        service.registerReceiver(vacantNotifyClearReceiver, clearIntentFilter)
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
            val sorted = TreeSet<PacketCollection>(Comparator { o1, o2 -> (o1.dateMillis - o2.dateMillis).toInt() })
            sorted.addAll(dataRequest.packetCollectionList)
            if(sorted.isEmpty()){
                println("no outhouse data!")
//                getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
            } else {
                val packetCollection = sorted.last()
                var occupancyPacket: OccupancyPacket? = null
                var weatherPacket: WeatherPacket? = null
                for(packet in packetCollection.packets){
                    if(packet is OuthousePacket){
                        when(packet.packetType){
                            OuthousePacketType.OCCUPANCY -> occupancyPacket = packet as OccupancyPacket
                            OuthousePacketType.WEATHER -> weatherPacket = packet as WeatherPacket
                            null -> throw NullPointerException()
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
                    .setContentTitle("Outhouse occupancy: " + if(occupancy != null) occupancy.modeName else "NO OCCUPANCY DATA")
                    .setContentText(if(weatherPacket == null) "no weather data" else "Temperature: ${getTemperatureString(weatherPacket.temperatureCelsius)} " +
                            "| Humidity: ${round(weatherPacket.humidityPercent.toDouble()).toInt()}%")
                    .setSubText(getConnectedSummary(dataRequest.host))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setWhen(packetCollection.dateMillis)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    builder.setGroup(getGroup(OUTHOUSE_NOTIFICATION_ID))
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
            }
        } else {
            println("unsuccessful outhouse data request")
//            getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
        }
    }
    private fun getTemperatureString(temperatureCelsius: Number): String {
        val f = temperatureCelsius.toDouble() * 9 / 5 + 32
        return "$f (F)"
    }

    override fun onTimeout() {
//        getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
    }

    override val updatePeriodType = UpdatePeriodType.SMALL_DATA
    override val startKey: Long
        get() = System.currentTimeMillis() - 5 * 60 * 1000

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
