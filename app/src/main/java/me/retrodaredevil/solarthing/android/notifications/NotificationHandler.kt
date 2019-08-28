package me.retrodaredevil.solarthing.android.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.text.Html
import android.text.Spanned
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarPacketInfo
import me.retrodaredevil.solarthing.android.getModeName
import me.retrodaredevil.solarthing.android.getOrderedValues
import me.retrodaredevil.solarthing.packets.Modes
import me.retrodaredevil.solarthing.solar.SolarPacket
import me.retrodaredevil.solarthing.solar.SolarPacketType
import me.retrodaredevil.solarthing.solar.common.ChargeController
import me.retrodaredevil.solarthing.solar.common.DailyData
import me.retrodaredevil.solarthing.solar.outback.OutbackPacket
import me.retrodaredevil.solarthing.solar.outback.fx.ACMode
import me.retrodaredevil.solarthing.solar.outback.fx.FXStatusPacket
import me.retrodaredevil.solarthing.solar.outback.fx.MiscMode
import me.retrodaredevil.solarthing.solar.outback.fx.OperationalMode
import me.retrodaredevil.solarthing.solar.outback.mx.AuxMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverErrorMode
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.StreetLight
import java.text.DateFormat
import java.util.*


object NotificationHandler {
    private const val SEPARATOR = "<span style=\"overflow-wrap:break-word\">|</span>"
    private const val DOUBLE_SEPARATOR = "<span style=\"overflow-wrap:break-word\">||</span>"
    private const val MX_COLOR = 0x000077

    private const val FX_COLOR_HEX_STRING = "#770000"
    private const val MX_COLOR_HEX_STRING = "#000077"
    private const val ROVER_COLOR_HEX_STRING = "#3e9ae9"

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
                "AC Input Voltage: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(info, it) + it.inputVoltage} + "\n" +
                "Charger Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(info, it) + it.chargerCurrent } + "\n" +
                "Buy Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(info, it) + it.buyCurrent }

        val title = if(acUseInfo == null){
            "Generator Starting"
        } else {
            val onText = if(acMode == ACMode.AC_DROP) "DROP" else "ON"
            "Generator $onText | " + when {
                info.fxMap.values.any { OperationalMode.FLOAT.isActive(it.operatingModeValue) } -> "float charge"
                info.fxMap.values.any { OperationalMode.EQ.isActive(it.operatingModeValue) } -> "EQ charge"
                info.fxMap.values.any { OperationalMode.CHARGE.isActive(it.operatingModeValue) } -> "charging"
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

    private fun getDeviceString(info: SolarPacketInfo, packet: SolarPacket): String{
        return when(packet.packetType){
            SolarPacketType.FX_STATUS -> "(<span style=\"color:$FX_COLOR_HEX_STRING\">${(packet as FXStatusPacket).address}</span>)"
            SolarPacketType.MXFM_STATUS -> "(<span style=\"color:$MX_COLOR_HEX_STRING\">${(packet as MXStatusPacket).address}</span>)"
            SolarPacketType.RENOGY_ROVER_STATUS -> "(<span style=\"color:$ROVER_COLOR_HEX_STRING\">${info.getRoverID(packet as RoverStatusPacket)}</span>)"
            null -> throw NullPointerException()
            else -> throw UnsupportedOperationException("${packet.packetType} not supported!")
        }
    }
    private fun fromHtml(text: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text.replace("\n", "<br/>"), 0)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(text.replace("\n", "<br/>"))
        }
    }

    /**
     *
     * @param info The SolarPacketInfo representing a simpler view of a PacketCollection
     * @param summary The sub text (or summary) of the notification.
     */
    fun createStatusNotification(context: Context, info: SolarPacketInfo, summary: String = "", extraInfoPendingIntent: PendingIntent? = null): Notification {
        val devicesString = getOrderedValues(info.deviceMap).joinToString("") {
            when (it) {
                is FXStatusPacket -> oneWord("[<strong>${it.address}</strong> <span style=\"color:$FX_COLOR_HEX_STRING\">FX</span>]")
                is MXStatusPacket -> oneWord("[<strong>${it.address}</strong> <span style=\"color:$MX_COLOR_HEX_STRING\">MX</span>]")
                is RoverStatusPacket -> oneWord("[<strong>${info.getRoverID(it)}</strong> <span style=\"color:$ROVER_COLOR_HEX_STRING\">RV</span>]")
                else -> it.toString()
            }
        }

        val inverterVoltageString = when {
            info.fxMap.isEmpty() -> ""
            info.fxMap.values.all { MiscMode.FX_230V_UNIT.isActive(it.misc)} -> "230V"
            info.fxMap.values.none { MiscMode.FX_230V_UNIT.isActive(it.misc) } -> "120V"
            else -> "???V"
        }
        val batteryTypeString = when {
            info.roverMap.isEmpty() -> ""
            else -> SEPARATOR + info.roverMap.values.first().batteryType.modeName
        }
        var auxCount = 0
        val auxModesString = run {
            val list = mutableListOf<String>()

            for(device in getOrderedValues(info.deviceMap)) {
                val string = when(device){
                    is FXStatusPacket -> {
                        val auxString = if (MiscMode.AUX_OUTPUT_ON.isActive(device.misc)) {
                            auxCount++
                            "ON"
                        } else {
                            "Off"
                        }
                        oneWord(getDeviceString(info, device) + auxString)
                    }
                    is MXStatusPacket -> {
                        val auxActive = AuxMode.isAuxModeActive(device.auxMode)
                        val auxDisabled = AuxMode.DISABLED.isActive(device.auxMode)
                        if (auxActive || !auxDisabled) {
                            auxCount++
                        }
                        val auxName = when {
                            auxDisabled -> if(auxActive) "ON" else "Off"
                            else -> device.auxModeName + (if (auxActive) "(ON)" else "")
                        }
                        oneWord(getDeviceString(info, device) + auxName)
                    }
                    is RoverStatusPacket -> {
                        val on = device.streetLightStatus == StreetLight.ON
                        if(on){
                            auxCount++
                        }
                        oneWord(getDeviceString(info, device) + (if(on) "ON" else "Off"))
                    }
                    else -> null
                }
                if(string != null) {
                    list.add(string)
                }
            }

            list.joinToString(SEPARATOR)
        }

        val fxACModesString = if(info.fxMap.values.map { it.acModeMode }.toSet().size > 1){
            getOrderedValues(info.fxMap).joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.acModeName}" }
        } else {
            info.acMode.modeName
        }
        val dailyKWHString = getOrderedValues(info.dailyDataMap).joinToString(SEPARATOR) { "${getDeviceString(info, it as SolarPacket)}${SolarPacketInfo.FORMAT.format(it.dailyKWH)}" }
        val modesString = getOrderedValues(info.deviceMap).joinToString(SEPARATOR) { "${getDeviceString(info, it)}${getModeName(it)}" }

        val pvWattagesString = getOrderedValues(info.chargeControllerMap).joinToString(SEPARATOR) {
            "${getDeviceString(info, it as SolarPacket)}${(it.pvCurrent.toDouble() * it.inputVoltage.toDouble()).toInt()}"
        }
        val chargerWattagesString = getOrderedValues(info.chargeControllerMap).joinToString(SEPARATOR) {
            "${getDeviceString(info, it as SolarPacket)}${it.chargingPower.toInt()}"
        }

        val fxWarningsString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.warningsString}" }
        val fxErrorsString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.errorsString}" }
        val mxErrorsString = info.mxMap.values.joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.errorsString}" }
        val roverErrorsString = info.roverMap.values.joinToString(SEPARATOR) {
            "${getDeviceString(info, it)}${Modes.toString(RoverErrorMode::class.java, it.errorMode)}"
        }

        val text = "" +
                "PV: $pvWattagesString | Total: <strong>${info.pvWattageString}</strong> W\n" +
                "Charger: $chargerWattagesString | " + oneWord("Total: <strong>${info.pvChargerWattageString}</strong> W") + "\n" +
                "Daily kWH: $dailyKWHString | " + oneWord("Total: <strong>${info.dailyKWHoursString}</strong>") + "\n" +
                "System: $devicesString$batteryTypeString\n" +
                (if(info.fxMap.values.any { it.errorMode != 0 }) "FX Errors: $fxErrorsString\n" else "") +
                (if(info.mxMap.values.any { it.errorMode != 0 }) "MX Errors: $mxErrorsString\n" else "") +
                (if(info.roverMap.values.any { it.activeErrors.isNotEmpty() }) "Rover Errors: $roverErrorsString\n" else "") +
                (if(info.fxMap.values.any { it.warningMode != 0 }) "FX Warn: $fxWarningsString\n" else "") +
                (if(info.fxMap.isNotEmpty()) { "$inverterVoltageString $SEPARATOR $fxACModesString $SEPARATOR Generator " + (if(info.acMode != ACMode.NO_AC) "<strong>ON</strong>" else "Off") + "\n" } else "") +
                "Mode: $modesString\n" +
                "Batt: " + getOrderedValues(info.batteryMap).joinToString(SEPARATOR) { getDeviceString(info, it as SolarPacket) + SolarPacketInfo.TENTHS_FORMAT.format(it.batteryVoltage) } + "$DOUBLE_SEPARATOR${info.estimatedBatteryVoltageString} V\n" +
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
            .setContentTitle("Battery: ${info.batteryVoltageString} V Load: ${info.loadString} W")
            .setContentText("pv:${info.pvWattageString} kwh:${info.dailyKWHoursString} err:${info.errorsCount}" + (if(info.hasWarnings) " warn:${info.warningsCount}" else "") +  (if(auxCount > 0) " aux:$auxCount" else "") + " generator:" + if(info.acMode != ACMode.NO_AC) "ON" else "off")
            .setStyle(style)
            .setOnlyAlertOnce(true)
            .setWhen(info.dateMillis)
            .setShowWhen(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Color.YELLOW)
            builder.setCategory(Notification.CATEGORY_STATUS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(extraInfoPendingIntent != null) {
                builder.addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.solar_panel),
                        "More",
                        extraInfoPendingIntent
                    ).build()
                )
            }
        }

        return builder.build()
    }
    fun createEndOfDay(context: Context, currentInfo: SolarPacketInfo, dailyData: DailyData, dateMillis: Long): Pair<Notification, Notification> {
        val builder = createNotificationBuilder(context, NotificationChannels.END_OF_DAY.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setContentTitle(when(dailyData){
                is MXStatusPacket -> "MX on port ${dailyData.address} end of day"
                is RoverStatusPacket -> "Rover ${currentInfo.getRoverID(dailyData)} ${dailyData.productSerialNumber} end of day"
                else -> error("dailyData: $dailyData is not supported!")
            })
            .setContentText("Got ${SolarPacketInfo.FORMAT.format(dailyData.dailyKWH)} kWH")
        val summary = createNotificationBuilder(context, NotificationChannels.END_OF_DAY.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(END_OF_DAY_GROUP)
            summary.setGroup(END_OF_DAY_GROUP).setGroupSummary(true)
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
            SolarPacketType.RENOGY_ROVER_STATUS -> "Rover"
            else -> device.packetType.toString()
        }
        val deviceName = when(device){
            is OutbackPacket -> "$name on port ${device.address}"
            is RoverStatusPacket -> "Rover with serial ${device.productSerialNumber}"
            else -> name
        }
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
    fun createMoreInfoNotification(context: Context, device: SolarPacket, dateMillis: Long, moreRoverInfoAction: String? = null): Pair<Notification, Notification?>{
        val builder = createNotificationBuilder(context, NotificationChannels.MORE_SOLAR_INFO.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
        val summary = createMoreInfoSummary(context, dateMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(MORE_SOLAR_INFO_GROUP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_STATUS)
            }
        }
        when(device){
            is OutbackPacket -> {
                when(device.packetType){
                    SolarPacketType.FX_STATUS -> {
                        device as FXStatusPacket
                        builder.setContentTitle("FX on port ${device.address}")
                        val batteryVoltage = SolarPacketInfo.TENTHS_FORMAT.format(device.batteryVoltage)
                        builder.setSubText("${batteryVoltage}V | ${device.operatingModeName} | ${device.acModeName} | Inv: ${device.inverterCurrent}A | ${getTimeString(dateMillis)}")
                        builder.style = Notification.BigTextStyle().bigText(
                            "Battery Voltage: $batteryVoltage\n" +
                                    "Operating Mode: ${device.operatingModeName} | " +
                                    "AC Mode: ${device.acModeName}\n" +
                                    "AC Output Voltage: ${device.outputVoltage}\n" +
                                    "Inverter Current: ${device.inverterCurrent} | Sell Current: ${device.sellCurrent}\n" +
                                    "AC Input Voltage: ${device.inputVoltage}\n" +
                                    "Buy Current: ${device.buyCurrent} | Charger Current: ${device.chargerCurrent}\n" +
                                    "Is 230V: " + (if(MiscMode.FX_230V_UNIT.isActive(device.misc)) "yes" else "no") + " | " +
                                    "Aux On: " + (if(MiscMode.AUX_OUTPUT_ON.isActive(device.misc)) "yes" else "no") + "\n" +
                                    "Errors: ${device.errorsString} Warnings: ${device.warningsString}\n" +
                                    "Check sum: ${device.chksum}"
                        )
                    }
                    SolarPacketType.MXFM_STATUS -> {
                        device as MXStatusPacket
                        builder.setContentTitle("MX on port ${device.address}")
                        val batteryVoltage = SolarPacketInfo.TENTHS_FORMAT.format(device.batteryVoltage)
                        builder.setSubText("$batteryVoltage | ${device.chargerModeName} | ${device.chargingCurrent}A | ${device.dailyKWH} kWH | ${getTimeString(dateMillis)}")
                        builder.style = Notification.BigTextStyle().bigText(
                            "Battery Voltage: $batteryVoltage\n" +
                                    createChargeControllerMoreInfo(device) +
                                    "Charger Mode: ${device.chargerModeName}\n" +
                                    "Aux Mode: ${device.auxModeName} | FM Aux On: " + (if(AuxMode.isAuxModeActive(device.auxMode)) "yes" else "no") + "\n" +
                                    "Errors: ${device.errorsString}\n" +
                                    "Daily kWH: ${device.dailyKWH} | " +
                                    "Daily AH: ${device.dailyAH}\n" +
                                    "Check sum: ${device.chksum}"
                        )
                    }
                    else -> throw IllegalArgumentException("$device not supported!")
                }
            }
            is RoverStatusPacket -> {
                builder.setContentTitle("Rover with serial: ${device.productSerialNumber}")
                val batteryVoltage = SolarPacketInfo.TENTHS_FORMAT.format(device.batteryVoltage)
                builder.setSubText("$batteryVoltage | ${device.chargingMode.modeName} | ${device.chargingCurrent}A | ${device.batteryTemperature}C | ${device.dailyKWH} kWH | ${getTimeString(dateMillis)}")
                builder.style = Notification.BigTextStyle().bigText(
                    "Battery Voltage: $batteryVoltage | SOC: ${device.batteryCapacitySOC}% | ${device.recognizedVoltage?.modeName ?: "??V"}/${device.systemVoltageSetting.modeName}\n" +
                            createChargeControllerMoreInfo(device) +
                            "Charging State: ${device.chargingMode.modeName} | Load Mode: ${device.loadWorkingModeValue}\n" +
                            "Errors: ${Modes.toString(RoverErrorMode::class.java, device.errorMode)}\n" +
                            "Temperature: Controller: ${device.controllerTemperature}C | Battery: ${device.batteryTemperature}C\n" +
                            "Day: ${device.operatingDaysCount} | kWH: ${device.dailyKWH} | AH: ${device.dailyAH} | " +
                            "Max: ${device.dailyMaxBatteryVoltage}V | Min: ${device.dailyMinBatteryVoltage}V\n" +
                            "Charge Max: ${device.dailyMaxChargingCurrent}A/${device.dailyMaxChargingPower}W"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    builder.addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(context, R.drawable.solar_panel),
                            "More",
                            PendingIntent.getBroadcast(
                                context,
                                0,
                                Intent(moreRoverInfoAction).apply {
                                    putExtra("rover_serial", device.productSerialNumber)
                                },
                                PendingIntent.FLAG_CANCEL_CURRENT
                            )
                        ).build()
                    )
                }
            }
            else -> throw IllegalArgumentException("$device not supported!")
        }
        return Pair(builder.build(), summary)
    }
    private fun createChargeControllerMoreInfo(device: ChargeController): String{
        return "Charging: Current: ${SolarPacketInfo.TENTHS_FORMAT.format(device.chargingCurrent)}A | " +
                "Power: ${SolarPacketInfo.TENTHS_FORMAT.format(device.chargingPower)}W\n" +
                "PV: Current: ${SolarPacketInfo.TENTHS_FORMAT.format(device.pvCurrent)}A * " +
                "Voltage: ${SolarPacketInfo.TENTHS_FORMAT.format(device.inputVoltage)}V = " +
                "Power: ${SolarPacketInfo.TENTHS_FORMAT.format(device.inputVoltage.toDouble() * device.pvCurrent.toDouble())}W\n"
    }
    fun createMoreRoverInfoNotification(context: Context, device: RoverStatusPacket, dateMillis: Long): Pair<Notification, Notification?>{
        val builder = createNotificationBuilder(context, NotificationChannels.MORE_SOLAR_INFO.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.setGroup(MORE_SOLAR_INFO_GROUP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_STATUS)
            }
        }
        val summary = createMoreInfoSummary(context, dateMillis)

        builder.setContentTitle("Rover with serial: ${device.productSerialNumber} | More Info")
        builder.style = Notification.BigTextStyle().bigText(
            "Max Voltage: ${device.maxVoltage.modeName} | Battery: Type: ${device.batteryType.modeName}\n" +
                    "Controller: Address: ${device.controllerDeviceAddress}\n" +
                    "No Charge Below 0C: " + (if(device.specialPowerControlE021.isNoChargingBelow0CEnabled) "yes" else "no") + "\n" +
                    "Hardware: ${device.hardwareVersion} | Software: ${device.softwareVersion}\n" +
                    "${device.productModel} | Charge: ${device.ratedChargingCurrentValue}A | Discharge: ${device.ratedDischargingCurrentValue}A\n" +
                    "Cumulative: kWH: ${device.cumulativeKWH} AH: ${device.chargingAmpHoursOfBatteryCount}\n" +
                    "Street light: ${device.streetLightStatus.modeName} Brightness: ${device.streetLightBrightnessPercent}"
        )

        return Pair(builder.build(), summary)
    }
    private fun createMoreInfoSummary(context: Context, dateMillis: Long): Notification? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val summary = createNotificationBuilder(context, NotificationChannels.MORE_SOLAR_INFO.id, null)
                .setSmallIcon(R.drawable.solar_panel)
                .setShowWhen(false)
                .setWhen(dateMillis)
                .setOnlyAlertOnce(true)
            summary.setGroup(MORE_SOLAR_INFO_GROUP).setGroupSummary(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                summary.setCategory(Notification.CATEGORY_STATUS)
            }
            return summary.build()
        }
        return null
    }
    private fun getTimeString(dateMillis: Long) = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(GregorianCalendar().apply { timeInMillis = dateMillis}.time)

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