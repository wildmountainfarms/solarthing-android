package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import me.retrodaredevil.solarthing.android.RecentData
import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketType
import me.retrodaredevil.solarthing.packet.StatusPacket
import me.retrodaredevil.solarthing.packet.fx.FXStatusPacket
import me.retrodaredevil.solarthing.packet.mxfm.MXFMStatusPacket

object NotificationHandler {

    fun updateStatusNotification(context: Context){
        val notification = createStatusNotification(context, RecentData.packetCollections)
        if(notification == null){
            getManager(context).cancel(0)
            return
        }
        getManager(context).notify(0, notification)
    }
    fun generatorAlert(context: Context){
        getManager(context).notify(1, createGeneratorAlert(context))
    }

    private fun getManager(context: Context) = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private fun createGeneratorAlert(context: Context): Notification {
        return createNotificationBuilder(context, NotificationChannels.GENERATOR_NOTIFICATION.id)
            .setSmallIcon(android.R.color.transparent)
            .setContentTitle("Generator")
            .setContentText("The generator needs to be turned off!")
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()
    }

    /**
     *
     * @param packetCollections The list of [PacketCollection]s where the last element is the most recent
     */
    private fun createStatusNotification(context: Context, packetCollections: List<PacketCollection>): Notification? {
        var dateMillis: Long? = null
        var batteryVoltageString: String? = null
        var loadString: String? = null
        var generatorOn: Boolean? = null
        var generatorToBatteryWattageString: String? = null
        var generatorWattageTotalString: String? = null
        var pvWattageString: String? = null

        for(packetCollection in packetCollections.reversed()){ // reversed - now first packets are most recent
            val fxMap = HashMap<Int, FXStatusPacket>()
            val mxfmMap = HashMap<Int, MXFMStatusPacket>()
            for(packet in packetCollection.packets){
                if(packet is StatusPacket){
                    when(packet.packetType){
                        PacketType.FX_STATUS -> {
                            val fx = packet as FXStatusPacket
                            fxMap[fx.address] = fx
                        }
                        PacketType.MXFM_STATUS -> {
                            val mx = packet as MXFMStatusPacket
                            mxfmMap[mx.address] = mx
                        }
                        PacketType.FLEXNET_DC_STATUS -> System.err.println("Not set up for FLEXNet packets!")
                        null -> throw NullPointerException("packetType is null! packet: $packet")
                    }
                }
            }
            if(fxMap.isEmpty() || mxfmMap.isEmpty()){
                continue
            }
            dateMillis = packetCollection.dateMillis
            var load = 0
            generatorOn = false
            var generatorToBatteryWattage = 0
            var generatorWattageTotal = 0
            for(fx in fxMap.values){
                batteryVoltageString = fx.batteryVoltageString
                load += fx.outputVoltage * fx.inverterCurrent
                if(generatorOn !== null && !generatorOn){
                    generatorOn = fx.chargerCurrent > 0
                }
                generatorToBatteryWattage += fx.inputVoltage * fx.chargerCurrent
                generatorWattageTotal += fx.inputVoltage * fx.buyCurrent
            }
            loadString = load.toString()
            generatorToBatteryWattageString = generatorToBatteryWattage.toString()
            generatorWattageTotalString = generatorWattageTotal.toString()

            var pvWattage: Int? = null
            for(mx in mxfmMap.values){
                pvWattage = mx.pvCurrent * mx.inputVoltage
            }
            pvWattageString = pvWattage.toString()
            break
        }
        if(dateMillis == null || batteryVoltageString == null || loadString == null || generatorOn == null
            || generatorToBatteryWattageString == null || generatorWattageTotalString == null || pvWattageString == null){
            return null
        }
        val isRecent = System.currentTimeMillis() - dateMillis < 1000 * 60 * 60 // true if within one hour
        val style = Notification.BigTextStyle()
            .bigText("Battery Voltage: $batteryVoltageString V\n" +
                    "Load: $loadString W\n" +
                    "Power from Solar Panels: $pvWattageString W" +
                    if(generatorOn)
                        "\nGenerator -> Battery: $generatorToBatteryWattageString W\n" +
                                "Generator Total: $generatorWattageTotalString W"
                    else "")

        return createNotificationBuilder(context, NotificationChannels.PERSISTENT_STATUS.id)
            .setSmallIcon(android.R.color.transparent)
            .setContentTitle("Solar Status")
            .setContentText("battery: $batteryVoltageString load: $loadString pv: $pvWattageString generator: " + if(generatorOn) "ON" else "OFF")
            .setStyle(style)
            .setOngoing(isRecent) // set ongoing if from the last hour
            .setOnlyAlertOnce(true)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .build()
    }
    private fun createNotificationBuilder(context: Context, channelId: String): Notification.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Notification.Builder(context, channelId)
        }
        return Notification.Builder(context)
    }
}