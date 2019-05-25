package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.support.v4.app.NotificationManagerCompat
import me.retrodaredevil.iot.outhouse.*
import me.retrodaredevil.iot.packets.Modes
import me.retrodaredevil.iot.packets.PacketCollection
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.OUTHOUSE_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.VACANT_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.request.DataRequest
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.math.round

private val vacantNotifyMap: MutableMap<Int, Boolean> = HashMap()

class OuthouseDataService(
    private val service: Service
) : DataService {
    override fun onInit() {
    }

    override fun onCancel() {
        getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
    }

    override fun onNewDataRequestLoadStart() {
    }

    override fun onDataRequest(dataRequest: DataRequest) {
        if(dataRequest.successful){
            val sorted = TreeSet<PacketCollection>(Comparator { o1, o2 -> (o1.dateMillis - o2.dateMillis).toInt() })
            sorted.addAll(dataRequest.packetCollectionList)
            if(sorted.isEmpty()){
                println("no outhouse data!")
                getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
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
                if(occupancy == Occupancy.OCCUPIED){
                    getManager().cancel(VACANT_NOTIFICATION_ID)
                }
                if(occupancy == Occupancy.VACANT && vacantNotifyMap[hashCode()] == true){
                    getManager().notify(VACANT_NOTIFICATION_ID,
                        getBuilder(NotificationChannels.VACANT_NOTIFICATION)
                            .setSmallIcon(R.drawable.potty)
                            .setContentTitle("Outhouse is now vacant!")
                            .setShowWhen(true)
                            .setWhen(packetCollection.dateMillis)
                            .build()
                    )
                    vacantNotifyMap[hashCode()] = false
                }
                val builder = getBuilder()
                    .setSmallIcon(if(occupancy == Occupancy.OCCUPIED) R.drawable.potty_occupied else R.drawable.potty)
                    .setContentTitle("Outhouse occupancy: " + if(occupancy != null) occupancy.modeName else "NO OCCUPANCY DATA")
                    .setContentText(if(weatherPacket == null) "no weather data" else "Temperature: ${getTemperatureString(weatherPacket.temperatureCelsius)} " +
                            "| Humidity: ${round(weatherPacket.humidityPercent.toDouble()).toInt()}%")
                    .setSubText(getConnectedSummary(dataRequest.host))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setWhen(packetCollection.dateMillis)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if(occupancy == Occupancy.OCCUPIED) {
                        val intent = Intent(service, EnableVacantNotification::class.java)
                        intent.putExtra("hashCode", hashCode())
                        val currentVacantNotify = vacantNotifyMap[hashCode()] ?: false
                        intent.putExtra("newValue", !currentVacantNotify)
                        val pendingIntent = PendingIntent.getBroadcast(
                            service, 0,
                            intent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                        )
                        builder.setContentIntent(pendingIntent)
                        builder.addAction(
                            Notification.Action.Builder(
                                Icon.createWithResource(service, R.drawable.potty),
                                if(currentVacantNotify) "Disable vacant notify" else "Enable vacant notify",
                                pendingIntent
                            ).build()
                        )
                    }
                }
                getManager().notify(OUTHOUSE_NOTIFICATION_ID, builder.build())
            }
        } else {
            println("unsuccessful outhouse data request")
            getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
        }
    }
    private fun getTemperatureString(temperatureCelsius: Number): String {
        val f = temperatureCelsius.toDouble() * 9 / 5 + 32
        return "$f (F)"
    }

    override fun onTimeout() {
        getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
    }

    override val updatePeriodType = UpdatePeriodType.SMALL_DATA
    override val startKey: Long
        get() = System.currentTimeMillis() - 5 * 60 * 1000

    override val shouldUpdate: Boolean
        get() = NotificationChannels.OUTHOUSE_STATUS.isCurrentlyEnabled(service)

    @SuppressWarnings("deprecated")
    private fun getBuilder(notificationChannels: NotificationChannels = NotificationChannels.OUTHOUSE_STATUS): Notification.Builder {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return Notification.Builder(service.applicationContext, notificationChannels.id)
        }
        return Notification.Builder(service.applicationContext)
    }
    private fun getManager() = service.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
}
class EnableVacantNotification : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent != null){
            val hashCode = intent.getIntExtra("hashCode", 0)
            val newValue = intent.getBooleanExtra("newValue", false)
            vacantNotifyMap[hashCode] = newValue
            println("set vacant notify map to $newValue")
        }
    }

}