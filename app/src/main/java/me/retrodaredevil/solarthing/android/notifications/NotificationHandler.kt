package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import me.retrodaredevil.solarthing.android.PacketInfo
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.packet.*
import me.retrodaredevil.solarthing.packet.fx.ACMode
import me.retrodaredevil.solarthing.packet.fx.FXStatusPacket
import me.retrodaredevil.solarthing.packet.fx.OperationalMode
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
     * @param summary The sub text (or summary) of the notification.
     */
    fun createStatusNotification(context: Context, info: PacketInfo, summary: String = ""): Notification? {
//        val packetCollection = packetCollections.last() // the most recent
//        val info = PacketInfo(packetCollection)

        var devicesString: String? = null

        var fxACModesString: String? = null
        var mxChargerModesString: String? = null
        var mxAuxModesString: String? = null

        var fxWarningsString: String? = null
        var fxErrorsString: String? = null
        var mxErrorsString: String? = null

        for(fx in info.fxMap.values){

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
        for(mx in info.mxMap.values){
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
        if(devicesString == null || mxChargerModesString == null || fxACModesString == null || mxAuxModesString == null){
            return null
        }
        val style = Notification.BigTextStyle()
            .bigText("Load: ${info.loadString} W\n" +
                    "Power from Solar Panels: ${info.pvWattageString} W\n" +
                    "Generator -> Battery: ${info.generatorToBatteryWattageString} W\n" +
                    "Generator Total: ${info.generatorTotalWattage} W\n" +
                    "Daily kWH: ${info.dailyKWHoursString}\n" +
                    "\n" +
                    "Devices: $devicesString\n" +
                    "FX AC Mode: $fxACModesString\n" +
                    "MX/FM Charger Mode: $mxChargerModesString\n" +
                    "MX/FM Aux Mode: $mxAuxModesString" +
                    (if(fxWarningsString != null) "\nFX Warn: $fxWarningsString" else "") +
                    (if(fxErrorsString != null) "\nFX Errors: $fxErrorsString" else "") +
                    (if(mxErrorsString != null) "\nMX/FM Errors: $mxErrorsString" else ""))

        return createNotificationBuilder(context, NotificationChannels.PERSISTENT_STATUS.id)
            .setSmallIcon(R.drawable.solar_panel)
            .setSubText(summary)
            .setContentTitle("Battery Voltage: ${info.batteryVoltageString} V")
            .setContentText("load: ${info.loadString} pv: ${info.pvWattageString} generator: " + if(info.generatorOn) "ON" else "OFF")
            .setStyle(style)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(info.dateMillis)
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