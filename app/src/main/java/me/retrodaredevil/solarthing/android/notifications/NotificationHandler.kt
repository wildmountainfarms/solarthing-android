package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.Spanned
import me.retrodaredevil.solarthing.android.SolarPacketInfo
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.solar.SolarPacket
import me.retrodaredevil.solarthing.solar.SolarPacketType
import me.retrodaredevil.solarthing.solar.fx.MiscMode
import me.retrodaredevil.solarthing.solar.mx.AuxMode
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


object NotificationHandler {
    private const val SEPARATOR = "<span style=\"overflow-wrap:break-word\">|</span>"
    private const val DOUBLE_SEPARATOR = "<span style=\"overflow-wrap:break-word\">||</span>"
    private const val MX_COLOR = "#000077"
    private const val FX_COLOR = "#770000"


    fun createFloatGeneratorAlert(context: Context, floatModeActivatedInfo: SolarPacketInfo, currentInfo: SolarPacketInfo, generatorFloatTimeMillis: Long): Notification {
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

        return createNotificationBuilder(context, NotificationChannels.GENERATOR_DONE_NOTIFICATION.id, GENERATOR_FLOAT_NOTIFICATION_ID)
            .setSmallIcon(R.drawable.power_button)
            .setContentTitle("Generator")
            .setContentText("Should have turned off at $turnOffAtString!")
            .setSubText("Float started at $turnedOnAtString")
            .setWhen(shouldHaveTurnedOffAt)
            .setUsesChronometer(true) // stopwatch from when the generator should have been turned off
            .build()
    }
    fun createDoneGeneratorAlert(context: Context, doneChargingInfo: SolarPacketInfo): Notification {
        val stoppedChargingAt = doneChargingInfo.dateMillis
        val stoppedChargingAtString = DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(GregorianCalendar().apply { timeInMillis = stoppedChargingAt }.time)

        return createNotificationBuilder(context, NotificationChannels.GENERATOR_DONE_NOTIFICATION.id, GENERATOR_FLOAT_NOTIFICATION_ID)
            .setSmallIcon(R.drawable.power_button)
            .setContentTitle("Generator")
            .setContentText("The generator stopped charging the batteries. Time to turn it off")
            .setSubText("Stopped charging at $stoppedChargingAtString")
            .setWhen(stoppedChargingAt)
            .setUsesChronometer(true) // stopwatch from when the generator should have been turned off
            .build()
    }
    fun createBatteryNotification(context: Context, currentInfo: SolarPacketInfo, critical: Boolean): Notification {
        val voltageString = currentInfo.batteryVoltageString
        val builder = createNotificationBuilder(context, NotificationChannels.BATTERY_NOTIFICATION.id, BATTERY_NOTIFICATION_ID)
            .setSmallIcon(R.drawable.power_button)
            .setContentText("The battery voltage is low! $voltageString!!!")
            .setWhen(currentInfo.dateMillis)
            .setShowWhen(true)
        if(critical) {
            builder.setContentTitle("CRITICAL BATTERY $voltageString V")
        } else {
            builder.setContentTitle("Low Battery $voltageString V")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_ERROR)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Color.RED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(critical){
                builder.setColorized(true) // TODO this won't do anything unless we convince android that this notification is important
            }
        }

        val r = builder.build()
        if(critical) {
            r.flags = r.flags or Notification.FLAG_INSISTENT
        }
        return r
    }

    fun createPersistentGenerator(context: Context, info: SolarPacketInfo,
                                  generatorTurnedOnInfo: SolarPacketInfo, floatModeActivatedInfo: SolarPacketInfo?,
                                  generatorFloatTimeMillis: Long): Notification{
        if(!info.generatorOn){
            throw IllegalStateException("Only call this method if the generator is on!")
        }
        val startTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(GregorianCalendar().apply { timeInMillis = generatorTurnedOnInfo.dateMillis }.time)
        val floatStartedText = if(floatModeActivatedInfo != null){
            val timeTurnedOnString = DateFormat.getTimeInstance(DateFormat.SHORT).format(
                GregorianCalendar().apply { timeInMillis = floatModeActivatedInfo.dateMillis }.time
            )
            if(floatModeActivatedInfo.isGeneratorInFloat(null)){
                "Float start at $timeTurnedOnString\n"
            } else {
                "Virtual float start at $timeTurnedOnString\n"
            }
        } else {
            ""
        }

        val text = "" +
                "Started at: $startTime\n" +
                floatStartedText +
                "Charger: ${info.generatorToBatteryWattageString} W\n" +
                "Total: ${info.generatorTotalWattageString} W\n" +
                "Pass Thru: ${info.generatorTotalWattage - info.generatorToBatteryWattage} W\n" +
                "AC Input Voltage: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.inputVoltage} + "\n" +
                "Charger Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.chargerCurrent } + "\n" +
                "Buy Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.buyCurrent }

        val builder = createNotificationBuilder(context, NotificationChannels.GENERATOR_PERSISTENT.id, GENERATOR_PERSISTENT_ID)
            .setSmallIcon(R.drawable.solar_panel)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentTitle("Generator ON")
            .setContentText("charger:${info.generatorToBatteryWattageString} total:${info.generatorTotalWattageString}")
            .setStyle(Notification.BigTextStyle().bigText(fromHtml(text)))
            .setShowWhen(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(0x654321)
        }
        if(floatModeActivatedInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            builder.setWhen(floatModeActivatedInfo.dateMillis + generatorFloatTimeMillis)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
        } else {
            builder.setWhen(generatorTurnedOnInfo.dateMillis)
            builder.setUsesChronometer(true)
        }
        return builder.build()
    }

    private fun getDeviceString(packet: SolarPacket): String{
        return when(packet.packetType){
            SolarPacketType.FX_STATUS -> "(<span style=\"color:$FX_COLOR\">${packet.address}</span>)"
            SolarPacketType.MXFM_STATUS -> "(<span style=\"color:$MX_COLOR\">${packet.address}</span>)"
            SolarPacketType.FLEXNET_DC_STATUS -> throw UnsupportedOperationException("FM not supported!")
            null -> throw NullPointerException()
        }
    }
    private fun fromHtml(text: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text.replace("\n", "<br/>"), 0)
        } else {
            Html.fromHtml(text.replace("\n", "<br/>"))
        }
    }

    /**
     *
     * @param info The SolarPacketInfo representing a simpler view of a PacketCollection
     * @param summary The sub text (or summary) of the notification.
     */
    fun createStatusNotification(context: Context, info: SolarPacketInfo, summary: String = ""): Notification {
        val devicesStringList = ArrayList<String>()
        devicesStringList.addAll(info.fxMap.values.map { "<span style=\"white-space: nowrap\">[<strong>${it.address}</strong> <span style=\"color:$FX_COLOR\">FX</span>]</span>" })
        devicesStringList.addAll(info.mxMap.values.map { "<span style=\"white-space: nowrap\">[<strong>${it.address}</strong> <span style=\"color:$MX_COLOR\">MX</span>]</span>" })

        val devicesString = devicesStringList.joinToString("")

        val unitVoltage = when {
            info.fxMap.values.all { MiscMode.FX_230V_UNIT.isActive(it.misc)} -> "230V"
            info.fxMap.values.none { MiscMode.FX_230V_UNIT.isActive(it.misc) } -> "120V"
            else -> "???V"
        }
        var auxCount = 0
        val auxModesString = run {
            var r = ""
            r += info.fxMap.values.joinToString(SEPARATOR) {
                val auxString = if (MiscMode.AUX_OUTPUT_ON.isActive(it.misc)) {
                    auxCount++
                    "ON"
                } else {
                    "Off"
                }
                getDeviceString(it) + auxString
            }
            r += DOUBLE_SEPARATOR
            r += info.mxMap.values.joinToString(SEPARATOR) {
                val auxActive = AuxMode.isAuxModeActive(it.auxMode)
                if(auxActive || (!AuxMode.UNKNOWN.isActive(it.auxMode) && !AuxMode.DISABLED.isActive(it.auxMode))){
                    auxCount++
                }
                getDeviceString(it) + it.auxModeName + (if(auxActive) "(ON)" else "")
            }
            r
        }

        val fxACModesString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(it)}${it.acModeName}" }
        val fxOperationalModeString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(it)}${it.operatingModeName}" }
        val mxDailyKWHString = info.mxMap.values.joinToString(SEPARATOR) { "${getDeviceString(it)}${SolarPacketInfo.FORMAT.format(it.dailyKWH)}" }
        val mxChargerModesString = info.mxMap.values.joinToString(SEPARATOR) { "${getDeviceString(it)}${it.chargerModeName}" }
        val mxPVWattagesString = info.mxMap.values.joinToString(SEPARATOR) {
            "${getDeviceString(it)}${it.pvCurrent * it.inputVoltage}"
        }
        val mxChargerWattagesString = info.mxMap.values.joinToString(SEPARATOR) {
            "${getDeviceString(it)}${(it.chargerCurrent * it.batteryVoltage).toInt()}"
        }

        val fxWarningsString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(it)}${it.warningsString}" }
        val fxErrorsString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(it)}${it.errorsString}" }
        val mxErrorsString = info.mxMap.values.joinToString(SEPARATOR) { "${getDeviceString(it)}${it.errorsString}" }

        val text = "" +
                "PV: $mxPVWattagesString | Total: <strong>${info.pvWattageString}</strong> W\n" +
                "Charger: $mxChargerWattagesString | Total: <strong>${info.pvChargerWattageString}</strong> W\n" +
                "Daily kWH: $mxDailyKWHString | Total: <strong>${info.dailyKWHoursString}</strong>\n" +
                "System: $devicesString$SEPARATOR$unitVoltage${SEPARATOR}Generator " + (if(info.generatorOn) "<strong>ON</strong>" else "Off") + "\n" +
                (if(info.fxMap.values.any { it.errorMode != 0 }) "FX Errors: $fxErrorsString\n" else "") +
                (if(info.mxMap.values.any { it.errorMode != 0 }) "MX Errors: $mxErrorsString\n" else "") +
                (if(info.fxMap.values.any { it.warningMode != 0 }) "FX Warn: $fxWarningsString\n" else "") +
                "AC Mode: $fxACModesString\n" +
                "Mode: $fxOperationalModeString$DOUBLE_SEPARATOR$mxChargerModesString\n" +
                "Aux: $auxModesString"
        if(text.length > 5 * 1024){
            System.err.println("bigText.length: ${text.length}! Some text may be cut off")
        }

        val style = Notification.BigTextStyle().bigText(fromHtml(text))

        val builder = createNotificationBuilder(context, NotificationChannels.SOLAR_STATUS.id, SOLAR_NOTIFICATION_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setSubText(summary)
            .setContentTitle("Battery Voltage: ${info.batteryVoltageString} V Load: ${info.loadString} W")
            .setContentText("pv:${info.pvWattageString} kWH:${info.dailyKWHoursString} err:${info.errorsCount} warn:${info.warningsCount} aux:$auxCount generator:" + if(info.generatorOn) "ON" else "off")
            .setStyle(style)
            .setOnlyAlertOnce(true)
            .setWhen(info.dateMillis)
            .setShowWhen(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Color.YELLOW)
            builder.setCategory(Notification.CATEGORY_STATUS)
        }

        return builder.build()
    }
    private fun createNotificationBuilder(context: Context, channelId: String, notificationId: Int): Notification.Builder {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(getGroup(notificationId))
        }
        return builder
    }
}