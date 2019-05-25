package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.content.Context
import android.os.Build
import me.retrodaredevil.solarthing.android.SolarPacketInfo
import me.retrodaredevil.solarthing.android.R
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

const val SEPARATOR = "|"

object NotificationHandler {


    fun createGeneratorAlert(context: Context, floatModeActivatedInfo: SolarPacketInfo, currentInfo: SolarPacketInfo, generatorFloatTimeMillis: Long): Notification {
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
     * @param info The SolarPacketInfo representing a simpler view of a PacketCollection
     * @param summary The sub text (or summary) of the notification.
     */
    fun createStatusNotification(context: Context, info: SolarPacketInfo, summary: String = "",
                                 floatModeActivatedInfo: SolarPacketInfo?, generatorFloatTimeMillis: Long): Notification {
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
            val timeLeft = (floatModeActivatedInfo.dateMillis + generatorFloatTimeMillis) - System.currentTimeMillis()
            val absTimeLeft = abs(timeLeft)
            val hours = TimeUnit.MILLISECONDS.toHours(absTimeLeft)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(absTimeLeft) - TimeUnit.HOURS.toMinutes(hours)
            val minutesString = minutes.toString()

            "$hours" +
                    ":" +
                    (if(minutesString.length == 1) "0$minutesString" else minutesString) +
                    " " +
                    (if(timeLeft < 0) "PAST" else "left") +
                    "\n"
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
        val generatorWattageText = if(info.generatorOn || info.generatorToBatteryWattage > 0 || info.generatorTotalWattage > 0){
            " to battery: ${info.generatorToBatteryWattageString} W | total: ${info.generatorTotalWattageString} W"
        } else {
            ""
        }

        val style = Notification.BigTextStyle()
            .bigText("Power from Solar Panels: ${info.pvWattageString} W | Daily kWH: ${info.dailyKWHoursString}\n" +
                    "Generator (${if(info.generatorOn) "ON" else "OFF"}) $timeLeftText" + generatorWattageText + "\n" +
                    (if(timeTurnedOnText.isNotEmpty()) timeTurnedOnText + "\n" else "") +
                    "FX Charger Current: ${info.fxChargerCurrentString}A|FX Buy Current: ${info.fxBuyCurrentString}A\n" +
                    "Devices: $devicesString\n" +
                    (if(info.fxMap.values.any { it.errorMode != 0 }) "FX Errors: $fxErrorsString\n" else "") +
                    (if(info.mxMap.values.any { it.errorMode != 0 }) "MX Errors: $mxErrorsString\n" else "") +
                    (if(info.fxMap.values.any { it.warningMode != 0 }) "FX Warn: $fxWarningsString\n" else "") +
                    "FX AC Mode: $fxACModesString\n" +
                    "FX Operational Mode: $fxOperationalModeString\n" +
                    "MX Charger Mode: $mxChargerModesString\n" +
                    "MX Aux Mode: $mxAuxModesString"
            )

        return createNotificationBuilder(context, NotificationChannels.PERSISTENT_STATUS.id)
            .setSmallIcon(R.drawable.solar_panel)
            .setSubText(summary)
            .setContentTitle("Battery Voltage: ${info.batteryVoltageString} V Load: ${info.loadString} W")
            .setContentText("pv:${info.pvWattageString} err:${info.errorsCount} warn:${info.warningsCount} generator:" + if(info.generatorOn) "ON $timeLeftText" else "OFF")
            .setStyle(style)
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