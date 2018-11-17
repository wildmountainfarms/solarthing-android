package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.packet.PacketCollection
import me.retrodaredevil.solarthing.packet.PacketType
import me.retrodaredevil.solarthing.packet.StatusPacket
import me.retrodaredevil.solarthing.packet.fx.FXStatusPacket
import me.retrodaredevil.solarthing.packet.mxfm.MXFMStatusPacket

object NotificationHandler {


    fun generatorAlert(context: Context){
        getManager(context).notify(2, createGeneratorAlert(context))
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
    fun createStatusNotification(context: Context, packetCollections: List<PacketCollection>): Notification? {
        var dateMillis: Long? = null
        var batteryVoltageString: String? = null
        var loadString: String? = null
        var generatorOn: Boolean? = null
        var generatorToBatteryWattageString: String? = null
        var generatorWattageTotalString: String? = null
        var pvWattageString: String? = null
        var calculatedSolarPanelWattageString: String? = null // beta

        var devicesString: String? = null

        var fxACModesString: String? = null
        var mxChargerModesString: String? = null
        var mxAuxModesString: String? = null

        var fxWarningsString: String? = null
        var fxErrorsString: String? = null
        var mxErrorsString: String? = null

        var dailyKWHString: String? = null

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
                    generatorOn = fx.buyCurrent > 0
                }
                generatorToBatteryWattage += fx.inputVoltage * fx.chargerCurrent
                generatorWattageTotal += fx.inputVoltage * fx.buyCurrent

                val deviceString = "[${fx.address} FX]"
                if(devicesString == null){
                    devicesString = deviceString
                } else {
                    devicesString += "|$deviceString"
                }

                val prefix = "(${fx.address})"

                val acModeString = "$prefix${fx.acModeName}"
                if(fxACModesString == null){
                    fxACModesString = acModeString
                } else {
                    fxACModesString += "|$acModeString"
                }

                val warningsString = fx.warningsString
                if(fxWarningsString == null){
                    if(warningsString.isNotEmpty()) {
                        fxWarningsString = prefix + warningsString
                    }
                } else {
                    fxWarningsString += "|$prefix$warningsString"
                }
                val errorsString = fx.errorsString
                if(fxErrorsString == null){
                    if(errorsString.isNotEmpty()) {
                        fxErrorsString = prefix + errorsString
                    }
                } else {
                    fxErrorsString += "|$prefix$errorsString"
                }
            }
            loadString = load.toString()
            generatorToBatteryWattageString = generatorToBatteryWattage.toString()
            generatorWattageTotalString = generatorWattageTotal.toString()

            var pvWattage: Int? = null
            var totalChargerCurrent: Float = 0f
            var totalVoltage: Int = 0
            for(mx in mxfmMap.values){
                pvWattage = mx.pvCurrent * mx.inputVoltage
                dailyKWHString = mx.dailyKWHString
                totalChargerCurrent += mx.chargerCurrent + mx.ampChargerCurrent // TODO this may not be current if multiple mx are connected
                totalVoltage += mx.inputVoltage

                val deviceString = "[${mx.address} MX/FM]"
                if(devicesString == null){
                    devicesString = deviceString
                } else {
                    devicesString += "|$deviceString"
                }

                val prefix = "(${mx.address})"

                val chargerModeString = "$prefix${mx.chargerModeName}"
                if(mxChargerModesString == null){
                    mxChargerModesString = chargerModeString
                } else {
                    mxChargerModesString += "|$chargerModeString"
                }
                val auxModeString = "$prefix${mx.auxModeName}"
                if(mxAuxModesString == null){
                    mxAuxModesString = auxModeString
                } else {
                    mxAuxModesString += "|$auxModeString"
                }

                val errorsString = mx.errorsString
                if(mxErrorsString == null){
                    if(errorsString.isNotEmpty()) {
                        mxErrorsString = prefix + errorsString
                    }
                } else {
                    mxErrorsString += "|$prefix$errorsString"
                }
            }
            pvWattageString = pvWattage.toString()
            val totalToBatteryWattage = totalChargerCurrent * totalVoltage
            val calculatedSolarPanelWattage = totalToBatteryWattage - generatorToBatteryWattage
            calculatedSolarPanelWattageString = calculatedSolarPanelWattage.toString()
            break
        }
        if(dateMillis == null || batteryVoltageString == null || loadString == null || generatorOn == null
            || generatorToBatteryWattageString == null || generatorWattageTotalString == null || pvWattageString == null
            || devicesString == null || mxChargerModesString == null || fxACModesString == null || mxAuxModesString == null){
            return null
        }
        val isRecent = System.currentTimeMillis() - dateMillis < 1000 * 60 * 60 // true if within one hour
        val style = Notification.BigTextStyle()
            .bigText("Load: $loadString W\n" +
                    "Power from Solar Panels: $pvWattageString W\n" +
                    "(Beta) Power from Solar Panels: $calculatedSolarPanelWattageString W\n" +
                    "Generator -> Battery: $generatorToBatteryWattageString W\n" +
                    "Generator Total: $generatorWattageTotalString W" +
                    (if(dailyKWHString != null) "\nDaily kWH: $dailyKWHString" else "") +
                    "\n\nDevices: $devicesString\n" +
                    "FX AC Mode: $fxACModesString\n" +
                    "MX/FM Charger Mode: $mxChargerModesString\n" +
                    "MX/FM Aux Mode: $mxAuxModesString" +
                    (if(fxWarningsString != null) "\nFX Warn: $fxWarningsString" else "") +
                    (if(fxErrorsString != null) "\nFX Errors: $fxErrorsString" else "") +
                    (if(mxErrorsString != null) "\nMX/FM Errors: $mxErrorsString" else ""))

        return createNotificationBuilder(context, NotificationChannels.PERSISTENT_STATUS.id)
            .setSmallIcon(R.drawable.solar_panel)
//            .setSubText("sub text")
            .setContentTitle("Battery Voltage: $batteryVoltageString V")
            .setContentText("load: $loadString pv: $pvWattageString generator: " + if(generatorOn) "ON" else "OFF")
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