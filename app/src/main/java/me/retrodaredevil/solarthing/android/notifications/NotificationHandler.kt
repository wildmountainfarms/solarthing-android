package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import me.retrodaredevil.solarthing.android.NotificationClearedReceiver
import me.retrodaredevil.solarthing.android.PacketInfo
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.millisToString
import java.text.DateFormat
import java.util.*

const val SEPARATOR = "|"

object NotificationHandler {


    fun createGeneratorAlert(context: Context, floatModeActivatedInfo: PacketInfo, currentInfo: PacketInfo, generatorFloatTimeMillis: Long): Notification {
        val shouldHaveTurnedOffAt = floatModeActivatedInfo.dateMillis + generatorFloatTimeMillis
        val now = System.currentTimeMillis()
        if(now < shouldHaveTurnedOffAt){
            throw IllegalArgumentException("The generator alert should be to alert someone to turn it off! " +
                    "Not to alert them when in the future they should turn it off.")
        }
        val turnedOnAtString = DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(GregorianCalendar().apply { timeInMillis = floatModeActivatedInfo.dateMillis }.time)
        val turnOffAtString = DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(GregorianCalendar().apply { timeInMillis = shouldHaveTurnedOffAt }.time)

        return createNotificationBuilder(context, NotificationChannels.GENERATOR_NOTIFICATION.id)
            .setSmallIcon(R.drawable.power_button)
            .setContentTitle("Generator")
            .setContentText("Should have turned off at $turnOffAtString!")
            .setSubText("Float started at $turnedOnAtString")
            .setWhen(shouldHaveTurnedOffAt)
            .setUsesChronometer(true) // stopwatch from when the generator should have been turned off
            .build()
    }

    /**
     *
     * @param info The PacketInfo representing a simpler view of a PacketCollection
     * @param summary The sub text (or summary) of the notification.
     */
    fun createStatusNotification(context: Context, info: PacketInfo, summary: String = "",
                                 floatModeActivatedInfo: PacketInfo?, generatorFloatTimeMillis: Long): Notification {
        val devicesStringList = ArrayList<String>()
        devicesStringList.addAll(info.fxMap.values.map { "[${it.address} FX]" })
        devicesStringList.addAll(info.mxMap.values.map { "[${it.address} MX]" })

        val devicesString = devicesStringList.joinToString(SEPARATOR)


        val fxACModesString = info.fxMap.values.joinToString(SEPARATOR) { "(${it.address})${it.acModeName}" }
        val fxOperationalModeString = info.fxMap.values.joinToString(SEPARATOR) { "(${it.address})${it.operatingModeName}" }
        val mxChargerModesString = info.mxMap.values.joinToString(SEPARATOR) { "(${it.address})${it.chargerModeName}" }
        val mxAuxModesString = info.mxMap.values.joinToString(SEPARATOR) { "(${it.address})${it.auxModeName}" }

        val fxWarningsString = info.fxMap.values.joinToString(SEPARATOR) { "(${it.address})${it.warningsString}" }
        val fxErrorsString = info.fxMap.values.joinToString(SEPARATOR) { "(${it.address})${it.errorsString}" }
        val mxErrorsString = info.mxMap.values.joinToString(SEPARATOR) { "(${it.address})${it.errorsString}" }

        val timeLeftText = if (floatModeActivatedInfo != null) {
            millisToString((floatModeActivatedInfo.dateMillis + generatorFloatTimeMillis) - System.currentTimeMillis())
        } else {
            ""
        }
        val timeTurnedOnText = if(floatModeActivatedInfo != null){
            val timeTurnedOnString = DateFormat.getTimeInstance(DateFormat.SHORT).format(
                GregorianCalendar().apply { timeInMillis = floatModeActivatedInfo.dateMillis }.time
            )
            "Generator entered float mode at $timeTurnedOnString"
        } else {
            ""
        }

        val style = Notification.BigTextStyle()
            .bigText("Load: ${info.loadString} W\n" +
                    "Power from Solar Panels: ${info.pvWattageString} W\n" +
                    "Generator is ${if(info.generatorOn) "ON" else "OFF"} $timeLeftText\n" +
                    (if(timeTurnedOnText.isNotEmpty()) timeTurnedOnText + "\n" else "") +
                    "Generator -> Battery: ${info.generatorToBatteryWattageString} W\n" +
                    "Generator Total: ${info.generatorTotalWattageString} W\n" +
                    "FX Charger Current: ${info.fxChargerCurrentString} A\n" +
                    "FX Buy Current: ${info.fxBuyCurrentString} A\n" +
                    "Daily kWH: ${info.dailyKWHoursString}\n" +
                    "Devices: $devicesString\n" +
                    (if(info.fxMap.values.any { it.errorMode != 0 }) "FX Errors: $fxErrorsString\n" else "") +
                    (if(info.mxMap.values.any { it.errorMode != 0 }) "MX Errors: $mxErrorsString\n" else "") +
                    (if(info.fxMap.values.any { it.warningMode != 0 }) "FX Warn: $fxWarningsString\n" else "") +
                    "FX AC Mode: $fxACModesString\n" +
                    "FX Operational Mode: $fxOperationalModeString\n" +
                    "MX Charger Mode: $mxChargerModesString\n" +
                    "MX Aux Mode: $mxAuxModesString"
            )

        val intent = Intent(context, NotificationClearedReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        return createNotificationBuilder(context, NotificationChannels.PERSISTENT_STATUS.id)
            .setSmallIcon(R.drawable.solar_panel)
            .setSubText(summary)
            .setContentTitle("Battery Voltage: ${info.batteryVoltageString} V")
            .setContentText("load: ${info.loadString} pv: ${info.pvWattageString} generator: " + if(info.generatorOn) "ON" else "OFF")
            .setStyle(style)
            .setOnlyAlertOnce(true)
            .setWhen(info.dateMillis)
            .setShowWhen(true)
            .setDeleteIntent(pendingIntent)
            .build()
    }
    private fun createNotificationBuilder(context: Context, channelId: String): Notification.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Notification.Builder(context, channelId)
        }
        return Notification.Builder(context)
    }
}