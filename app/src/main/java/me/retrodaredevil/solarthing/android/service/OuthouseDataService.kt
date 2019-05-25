package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import me.retrodaredevil.iot.outhouse.*
import me.retrodaredevil.iot.packets.Modes
import me.retrodaredevil.iot.packets.PacketCollection
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.OUTHOUSE_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.request.DataRequest
import java.util.*
import kotlin.Comparator
import kotlin.math.round

class OuthouseDataService(
    private val service: Service
) : DataService {
    override fun onInit() {
    }

    override fun onEnd() {
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
                notify(
                    getBuilder()
                        .setSmallIcon(R.drawable.solar_panel) // TODO change icon
                        .setContentTitle("Outhouse Status" + if(occupancyPacket != null) " | Occupancy: ${Modes.getActiveMode(
                            Occupancy::class.java, occupancyPacket.occupancy).modeName}" else "")
                        .setContentText(if(weatherPacket == null) "no weather data" else "Temperature: ${weatherPacket.temperatureCelsius} (C) " +
                                "| Humidity: ${round(weatherPacket.humidityPercent.toDouble()).toInt()}%")
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build()
                )
            }
        } else {
            println("unsuccessful outhouse data request")
            getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
        }
    }

    override fun onTimeout() {
        getManager().cancel(OUTHOUSE_NOTIFICATION_ID)
    }

    override val updatePeriodType = UpdatePeriodType.SMALL_DATA
    override val startKey: Long
        get() = System.currentTimeMillis() - 5 * 60 * 1000

    private fun notify(notification: Notification){
        getManager().notify(OUTHOUSE_NOTIFICATION_ID, notification)
//        service.startForeground(OUTHOUSE_NOTIFICATION_ID, notification)
    }
    @SuppressWarnings("deprecated")
    private fun getBuilder(): Notification.Builder {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return Notification.Builder(service.applicationContext, NotificationChannels.PERSISTENT_STATUS.id)
        }
        return Notification.Builder(service.applicationContext)
    }
    private fun getManager() = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}