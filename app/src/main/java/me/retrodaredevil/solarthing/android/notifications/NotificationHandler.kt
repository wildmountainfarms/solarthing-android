package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.Spanned
import me.retrodaredevil.solarthing.android.SolarPacketInfo
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.packets.Modes
import me.retrodaredevil.solarthing.solar.SolarPacket
import me.retrodaredevil.solarthing.solar.SolarPacketType
import me.retrodaredevil.solarthing.solar.fx.ACMode
import me.retrodaredevil.solarthing.solar.fx.MiscMode
import me.retrodaredevil.solarthing.solar.fx.OperationalMode
import me.retrodaredevil.solarthing.solar.mx.AuxMode
import me.retrodaredevil.solarthing.solar.mx.MXStatusPacket
import java.text.DateFormat
import java.util.*


object NotificationHandler {
    private const val SEPARATOR = "<span style=\"overflow-wrap:break-word\">|</span>"
    private const val DOUBLE_SEPARATOR = "<span style=\"overflow-wrap:break-word\">||</span>"
    private const val MX_COLOR = 0x000077
    private const val MX_COLOR_HEX_STRING = "#000077"
    private const val FX_COLOR_HEX_STRING = "#770000"

    private fun oneWord(string: String): String {
        return "<span style=\"white-space: nowrap\">$string</span>"
    }

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

        val builder = createNotificationBuilder(context, NotificationChannels.GENERATOR_DONE_NOTIFICATION.id, GENERATOR_FLOAT_NOTIFICATION_ID)
            .setSmallIcon(R.drawable.power_button)
            .setContentTitle("Generator")
            .setContentText("Should have turned off at $turnOffAtString!")
            .setSubText("Float started at $turnedOnAtString")
            .setWhen(shouldHaveTurnedOffAt)
            .setUsesChronometer(true) // stopwatch from when the generator should have been turned off
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_ALARM)
        }

        return builder.build()
    }
    fun createDoneGeneratorAlert(context: Context, doneChargingInfo: SolarPacketInfo): Notification {
        val stoppedChargingAt = doneChargingInfo.dateMillis
        val stoppedChargingAtString = DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(GregorianCalendar().apply { timeInMillis = stoppedChargingAt }.time)

        return createNotificationBuilder(context, NotificationChannels.GENERATOR_DONE_NOTIFICATION.id, GENERATOR_FLOAT_NOTIFICATION_ID)
            .setSmallIcon(R.drawable.power_button)
            .setContentTitle("Generator")
            .setContentText("The generator stopped charging the batteries")
            .setSubText("Stopped charging at $stoppedChargingAtString")
            .setWhen(stoppedChargingAt)
            .setUsesChronometer(true) // stopwatch from when the generator should have been turned off
//            .apply {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//                    setSortKey("b")
//                }
//            }
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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//            builder.setSortKey("a")
//        }

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

    /**
     * @param context the context
     * @param info The current packet
     * @param beginningACDropInfo The packet where the ac mode is [ACMode.AC_DROP] but packets before this packet the ac mode was [ACMode.NO_AC]
     * @param lastACDropInfo The last packet where the ac mode is [ACMode.AC_DROP]. This is normally the same as [lastACDropInfo] but may be a more recent packet
     * @param acUseInfo The first packet where the ac mode is [ACMode.AC_USE]. If [beginningACDropInfo] is not null, this should be right after it.
     * @param floatModeActivatedInfo The packet where float or virtual float was activated
     * @param generatorFloatTimeMillis The amount of time in milliseconds that the generator stays in float mode
     * @param uncertainGeneratorStartInfo true if we are unsure that [acUseInfo] is actually the first packet while the generator was running
     */
    fun createPersistentGenerator(context: Context, info: SolarPacketInfo,
                                  beginningACDropInfo: SolarPacketInfo?, lastACDropInfo: SolarPacketInfo?,
                                  acUseInfo: SolarPacketInfo?,
                                  floatModeActivatedInfo: SolarPacketInfo?,
                                  generatorFloatTimeMillis: Long,
                                  uncertainGeneratorStartInfo: Boolean): Notification{
        val acMode = info.acMode
        if(acMode == ACMode.NO_AC) throw IllegalStateException("Only call this method if the generator is on!")

        val acDropStartString = if(beginningACDropInfo != null){
            val startTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(GregorianCalendar().apply { timeInMillis = beginningACDropInfo.dateMillis }.time)
            "Generator (AC Drop) started at: $startTime" + (if(uncertainGeneratorStartInfo) " (uncertain start time)" else "") +"\n"
        } else ""
        val acUseStartString = if(acUseInfo != null){
            val startTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(GregorianCalendar().apply { timeInMillis = acUseInfo.dateMillis }.time)
            "AC Use started at: $startTime" + (if(uncertainGeneratorStartInfo) " (uncertain start time)" else "") +"\n"
        } else ""
        val lastACDropString = if(lastACDropInfo != null && acUseInfo != null && lastACDropInfo.dateMillis > acUseInfo.dateMillis){
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(GregorianCalendar().apply { timeInMillis = lastACDropInfo.dateMillis }.time)
            "Last AC Drop at $time\n"
        } else ""

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
        val passThru = info.generatorTotalWattage - info.generatorToBatteryWattage

        val text = "" +
                acDropStartString +
                acUseStartString +
                lastACDropString +
                floatStartedText +
                "Charger: ${info.generatorToBatteryWattageString} W\n" +
                "Total: ${info.generatorTotalWattageString} W\n" +
                "Pass Thru: $passThru W\n" +
                "AC Input Voltage: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.inputVoltage} + "\n" +
                "Charger Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.chargerCurrent } + "\n" +
                "Buy Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.buyCurrent }

        val title = if(acUseInfo == null){
            "Generator Starting"
        } else {
            val onText = if(acMode == ACMode.AC_DROP) "DROP" else "ON"
            "Generator $onText | " + when {
                info.fxMap.values.any { OperationalMode.FLOAT.isActive(it.operatingMode) } -> "float charge"
                info.fxMap.values.any { OperationalMode.EQ.isActive(it.operatingMode) } -> "EQ charge"
                info.fxMap.values.any { OperationalMode.CHARGE.isActive(it.operatingMode) } -> "normal charge"
                else -> "not charging"
            }
        }

        val builder = createNotificationBuilder(context, NotificationChannels.GENERATOR_PERSISTENT.id, GENERATOR_PERSISTENT_ID)
            .setSmallIcon(R.drawable.solar_panel)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentTitle(title)
            .setContentText("charger:${info.generatorToBatteryWattageString} total:${info.generatorTotalWattageString} pass thru:$passThru")
            .setStyle(Notification.BigTextStyle().bigText(fromHtml(text)))
            .setShowWhen(true)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//            builder.setSortKey("c")
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(0x654321) // brown
        }
        if(floatModeActivatedInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            builder.setWhen(floatModeActivatedInfo.dateMillis + generatorFloatTimeMillis)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
        } else {
            if(acUseInfo != null) {
                builder.setWhen(acUseInfo.dateMillis)
            } else {
                builder.setWhen(beginningACDropInfo!!.dateMillis)
            }
            builder.setUsesChronometer(true)
        }
        return builder.build()
    }

    private fun getDeviceString(packet: SolarPacket): String{
        return when(packet.packetType){
            SolarPacketType.FX_STATUS -> "(<span style=\"color:$FX_COLOR_HEX_STRING\">${packet.address}</span>)"
            SolarPacketType.MXFM_STATUS -> "(<span style=\"color:$MX_COLOR_HEX_STRING\">${packet.address}</span>)"
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
        devicesStringList.addAll(info.fxMap.values.map { oneWord("[<strong>${it.address}</strong> <span style=\"color:$FX_COLOR_HEX_STRING\">FX</span>]") })
        devicesStringList.addAll(info.mxMap.values.map { oneWord("[<strong>${it.address}</strong> <span style=\"color:$MX_COLOR_HEX_STRING\">MX</span>]") })

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
                if(auxActive || !AuxMode.DISABLED.isActive(it.auxMode)){
                    auxCount++
                }
                oneWord(getDeviceString(it) + it.auxModeName + (if(auxActive) "(ON)" else ""))
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
                "Charger: $mxChargerWattagesString | " + oneWord("Total: <strong>${info.pvChargerWattageString}</strong> W") + "\n" +
                "Daily kWH: $mxDailyKWHString | " + oneWord("Total: <strong>${info.dailyKWHoursString}</strong>") + "\n" +
                "System: $devicesString$SEPARATOR$unitVoltage\n" +
                (if(info.fxMap.values.any { it.errorMode != 0 }) "FX Errors: $fxErrorsString\n" else "") +
                (if(info.mxMap.values.any { it.errorMode != 0 }) "MX Errors: $mxErrorsString\n" else "") +
                (if(info.fxMap.values.any { it.warningMode != 0 }) "FX Warn: $fxWarningsString\n" else "") +
                "AC: $fxACModesString $SEPARATOR Generator " + (if(info.acMode != ACMode.NO_AC) "<strong>ON</strong>" else "Off") + "\n" +
                "Mode: $fxOperationalModeString$DOUBLE_SEPARATOR$mxChargerModesString\n" +
                "Aux: $auxModesString\n" +
                "Battery: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.batteryVoltageString} + DOUBLE_SEPARATOR + info.mxMap.values.joinToString(SEPARATOR) { getDeviceString(it) + it.batteryVoltageString} + "${DOUBLE_SEPARATOR}est: ${info.estimatedBatteryVoltageString} V"
        if(text.length > 5 * 1024){
            System.err.println("bigText.length: ${text.length}! Some text may be cut off")
        }

        val style = Notification.BigTextStyle().bigText(fromHtml(text))

        val builder = createNotificationBuilder(context, NotificationChannels.SOLAR_STATUS.id, SOLAR_NOTIFICATION_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setSubText(summary)
            .setContentTitle("Battery: ${info.batteryVoltageString} V Load: ${info.loadString} W")
            .setContentText("pv:${info.pvWattageString} kwh:${info.dailyKWHoursString} err:${info.errorsCount} warn:${info.warningsCount}" +  (if(auxCount > 0) " aux:$auxCount" else "") + " generator:" + if(info.acMode != ACMode.NO_AC) "ON" else "off")
            .setStyle(style)
            .setOnlyAlertOnce(true)
            .setWhen(info.dateMillis)
            .setShowWhen(true)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//            builder.setSortKey("d")
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Color.YELLOW)
            builder.setCategory(Notification.CATEGORY_STATUS)
        }

        return builder.build()
    }
    fun createMXEndOfDay(context: Context, mx: MXStatusPacket, dateMillis: Long): Pair<Notification, Notification> {
        val builder = createNotificationBuilder(context, NotificationChannels.MX_END_OF_DAY.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setContentTitle("MX on port ${mx.address} end of day")
            .setContentText("Got ${SolarPacketInfo.FORMAT.format(mx.dailyKWH)} kWH")
        val summary = createNotificationBuilder(context, NotificationChannels.MX_END_OF_DAY.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//            builder.setSortKey("b")
            builder.setGroup(MX_END_OF_DAY_GROUP)
            summary.setGroup(MX_END_OF_DAY_GROUP).setGroupSummary(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(MX_COLOR)
            builder.setCategory(Notification.CATEGORY_STATUS)
            summary.setColor(MX_COLOR)
            summary.setCategory(Notification.CATEGORY_STATUS)
        }


        return Pair(builder.build(), summary.build())
    }

    /**
     * @param context The context
     * @param device The most recent status packet representing a device that has been connected or disconnected
     * @param justConnected true if [device] has just been connected, false if [device] has just been disconnected
     */
    fun createDeviceConnectionStatus(context: Context, device: SolarPacket, justConnected: Boolean, dateMillis: Long): Pair<Notification, Notification>{
        val name = when(device.packetType){
            SolarPacketType.FX_STATUS -> "FX"
            SolarPacketType.MXFM_STATUS -> "MX"
            else -> device.packetType.toString()
        }
        val deviceName = "$name on port ${device.address}"
        val builder = createNotificationBuilder(context, NotificationChannels.CONNECTION_STATUS.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setContentTitle(deviceName + " " + (if(justConnected) "connected" else "disconnected"))

        val summary = createNotificationBuilder(context, NotificationChannels.CONNECTION_STATUS.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(DEVICE_CONNECTION_STATUS_GROUP)
            summary.setGroup(DEVICE_CONNECTION_STATUS_GROUP).setGroupSummary(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
            summary.setCategory(Notification.CATEGORY_STATUS)
        }

        return Pair(builder.build(), summary.build())
    }
    private fun createNotificationBuilder(context: Context, channelId: String, notificationId: Int?): Notification.Builder {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }
        if(notificationId != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                builder.setGroup(getGroup(notificationId))
            }
        }
        return builder
    }
}