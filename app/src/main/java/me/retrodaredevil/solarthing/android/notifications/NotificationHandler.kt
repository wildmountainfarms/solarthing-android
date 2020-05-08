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
import me.retrodaredevil.solarthing.android.*
import me.retrodaredevil.solarthing.android.data.*
import me.retrodaredevil.solarthing.android.util.Formatting
import me.retrodaredevil.solarthing.android.util.wattsToKilowattsString
import me.retrodaredevil.solarthing.packets.DocumentedPacket
import me.retrodaredevil.solarthing.packets.Modes
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment
import me.retrodaredevil.solarthing.packets.support.Support
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.solar.SolarStatusPacketType
import me.retrodaredevil.solarthing.solar.common.BasicChargeController
import me.retrodaredevil.solarthing.solar.common.DailyChargeController
import me.retrodaredevil.solarthing.solar.extra.SolarExtraPacketType
import me.retrodaredevil.solarthing.solar.outback.OutbackData
import me.retrodaredevil.solarthing.solar.outback.fx.ACMode
import me.retrodaredevil.solarthing.solar.outback.fx.FXStatusPacket
import me.retrodaredevil.solarthing.solar.outback.fx.MiscMode
import me.retrodaredevil.solarthing.solar.outback.fx.OperationalMode
import me.retrodaredevil.solarthing.solar.outback.fx.charge.FXChargingMode
import me.retrodaredevil.solarthing.solar.outback.fx.event.FXDayEndPacket
import me.retrodaredevil.solarthing.solar.outback.mx.AuxMode
import me.retrodaredevil.solarthing.solar.outback.mx.MXStatusPacket
import me.retrodaredevil.solarthing.solar.outback.mx.event.MXDayEndPacket
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

    fun createVoltageTimerAlert(context: Context, voltageTimerActivatedInfo: SolarPacketInfo, currentInfo: SolarPacketInfo, voltageTimerTimeMillis: Long): Notification {
        val shouldHaveTurnedOffAt = voltageTimerActivatedInfo.dateMillis + voltageTimerTimeMillis
        val now = System.currentTimeMillis()
        if(now < shouldHaveTurnedOffAt){
            throw IllegalArgumentException("The generator alert should be to alert someone to turn it off! " +
                    "Not to alert them when in the future they should turn it off.")
        }
        val turnedOnAtString = DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(GregorianCalendar().apply { timeInMillis = voltageTimerActivatedInfo.dateMillis }.time)
        val turnOffAtString = DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(GregorianCalendar().apply { timeInMillis = shouldHaveTurnedOffAt }.time)

        // TODO make this use a different NotificationChannel
        val builder = createNotificationBuilder(context, NotificationChannels.GENERATOR_DONE_NOTIFICATION.id, VOLTAGE_TIMER_NOTIFICATION_ID)
                .setSmallIcon(R.drawable.power_button)
                .setContentTitle("Generator")
                .setContentText("Should have turned off at $turnOffAtString!")
                .setSubText("Voltage Timer started at $turnedOnAtString")
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

        return createNotificationBuilder(context, NotificationChannels.GENERATOR_DONE_NOTIFICATION.id, GENERATOR_DONE_NOTIFICATION_ID)
                .setSmallIcon(R.drawable.power_button)
                .setContentTitle("Generator")
                .setContentText("The generator stopped charging the batteries")
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
    private fun millisToHoursString(millis: Long): String {
        val hours = millis / (60 * 60 * 1000.0)
        return Formatting.HUNDREDTHS.format(hours)
    }

    /**
     * @param context the context
     * @param info The current packet
     * @param beginningACDropInfo The packet where the ac mode is [ACMode.AC_DROP] but packets before this packet the ac mode was [ACMode.NO_AC]
     * @param lastACDropInfo The last packet where the ac mode is [ACMode.AC_DROP]. This is normally the same as [lastACDropInfo] but may be a more recent packet
     * @param acUseInfo The first packet where the ac mode is [ACMode.AC_USE]. If [beginningACDropInfo] is not null, this should be right after it.
     * @param voltageTimerStartedInfo The packet where the voltage timer was started
     * @param uncertainGeneratorStartInfo true if we are unsure that [acUseInfo] is actually the first packet while the generator was running
     */
    fun createPersistentGenerator(
            context: Context, info: SolarPacketInfo,
            beginningACDropInfo: SolarPacketInfo?, lastACDropInfo: SolarPacketInfo?,
            acUseInfo: SolarPacketInfo?,
            voltageTimerStartedInfo: SolarPacketInfo?,
            uncertainGeneratorStartInfo: Boolean
    ): Notification{
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

        val voltageTimerStartedText = if(voltageTimerStartedInfo != null){
            val timeTurnedOnString = DateFormat.getTimeInstance(DateFormat.SHORT).format(
                GregorianCalendar().apply { timeInMillis = voltageTimerStartedInfo.dateMillis }.time
            )
            "Voltage timer start at $timeTurnedOnString\n"
        } else {
            ""
        }
        val passThru = info.generatorTotalWattage - info.generatorToBatteryWattage

        val text = "" +
                acDropStartString +
                acUseStartString +
                lastACDropString +
                voltageTimerStartedText +
                "Charger: ${wattsToKilowattsString(info.generatorToBatteryWattage)} kW\n" +
                "Total: ${wattsToKilowattsString(info.generatorTotalWattage)} kW\n" +
                "Pass Thru: ${wattsToKilowattsString(passThru)} kW\n" +
                "AC Input Voltage: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(info, it) + it.inputVoltage} + "\n" +
                "Charger Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(info, it) + Formatting.OPTIONAL_TENTHS.format(it.chargerCurrent) } + "\n" +
                "Buy Current: " + info.fxMap.values.joinToString(SEPARATOR) { getDeviceString(info, it) + Formatting.OPTIONAL_TENTHS.format(it.buyCurrent) }

        val builder = createNotificationBuilder(context, NotificationChannels.GENERATOR_PERSISTENT.id, GENERATOR_PERSISTENT_ID)
            .setSmallIcon(R.drawable.solar_panel)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentText("charger:${wattsToKilowattsString(info.generatorToBatteryWattage)} total:${wattsToKilowattsString(info.generatorTotalWattage)} pass thru:${wattsToKilowattsString(passThru)}")
            .setStyle(Notification.BigTextStyle().bigText(fromHtml(text)))
            .setShowWhen(true)

        val title = if(acUseInfo == null){
            builder.setWhen(beginningACDropInfo!!.dateMillis)
            "Generator Starting"
        } else {
            var remainingTime: Long? = null
            if(acMode == ACMode.AC_USE){
                val operationalMode = (info.masterFXStatusPacket ?: error("AC Mode is use, we should have a master FX!")).operationalMode

                val fxChargingPacket = info.fxChargingPacket
                if(fxChargingPacket != null){
                    "Generator | " + when(val mode = fxChargingPacket.fxChargingMode){
                        FXChargingMode.BULK_TO_ABSORB -> "Bulk"
                        FXChargingMode.BULK_TO_EQ -> "Bulk to EQ"
                        FXChargingMode.ABSORB -> {
                            remainingTime = fxChargingPacket.remainingAbsorbTimeMillis
                            "Absorb (${millisToHoursString(remainingTime)})"
                        }
                        FXChargingMode.EQ -> {
                            remainingTime = fxChargingPacket.remainingEqualizeTimeMillis
                            "EQ (${millisToHoursString(remainingTime)})"
                        }
                        FXChargingMode.SILENT -> "not charging (Silent)"
                        FXChargingMode.REFLOAT -> "ReFloat"
                        FXChargingMode.FLOAT -> {
                            remainingTime = fxChargingPacket.remainingFloatTimeMillis
                            "Float (${millisToHoursString(remainingTime)})"
                        }
                        null -> "not charging (${operationalMode.modeName})"
                        else -> error("Unsupported mode: $mode")
                    }
                } else {
                    "Generator | " + when (operationalMode) {
                        OperationalMode.FLOAT -> "ReFloat/Float"
                        OperationalMode.EQ -> "Bulk/EQ"
                        OperationalMode.CHARGE -> "Bulk/Absorb"
                        else -> "not charging (${operationalMode.modeName})"
                    }
                }.also {
                    if (remainingTime != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        builder.setWhen(info.dateMillis + remainingTime)
                        builder.setUsesChronometer(true)
                        builder.setChronometerCountDown(true)
                    } else {
                        builder.setWhen(acUseInfo.dateMillis)
                    }
                }
            } else { // DROP
                builder.setWhen(acUseInfo.dateMillis)
                "Generator DROP"
            }
        }
        builder.setContentTitle(title)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(0x654321) // brown
        }
        return builder.build()
    }

    private fun getDeviceString(info: SolarPacketInfo, packet: DocumentedPacket<*>, includeParenthesis: Boolean = true): String{
        @Suppress("UsePropertyAccessSyntax")
        val r = when(val packetType = packet.getPacketType()){
            SolarStatusPacketType.FX_STATUS -> "<span style=\"color:$FX_COLOR_HEX_STRING\">${(packet as OutbackData).address}</span>"
            SolarStatusPacketType.MXFM_STATUS, SolarExtraPacketType.MXFM_DAILY -> "<span style=\"color:$MX_COLOR_HEX_STRING\">${(packet as OutbackData).address}</span>"
            SolarStatusPacketType.RENOGY_ROVER_STATUS -> "<span style=\"color:$ROVER_COLOR_HEX_STRING\">${info.getRoverId(packet as RoverStatusPacket)}</span>"
            null -> throw NullPointerException()
            else -> throw UnsupportedOperationException("$packetType not supported!")
        }
        return if(includeParenthesis)
            "($r)"
        else
            r
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
    fun createStatusNotification(context: Context, info: SolarPacketInfo, dailyInfo: SolarDailyInfo, summary: String, extraInfoPendingIntent: PendingIntent?, temperatureUnit: TemperatureUnit): Notification {
        val devicesString = getOrderedValues(
            info.deviceMap
        ).joinToString("") {
            when (it) {
                is FXStatusPacket -> oneWord("[<strong>${it.address}</strong> <span style=\"color:$FX_COLOR_HEX_STRING\">FX</span>]")
                is MXStatusPacket -> oneWord("[<strong>${it.address}</strong> <span style=\"color:$MX_COLOR_HEX_STRING\">MX</span>]")
                is RoverStatusPacket -> oneWord("[<strong>${info.getRoverId(it)}</strong> <span style=\"color:$ROVER_COLOR_HEX_STRING\">RV</span>]")
                else -> it.toString()
            }
        }

        val batteryTemperatureString = when {
            info.roverMap.isEmpty() -> ""
            else -> SEPARATOR + Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(info.roverMap.values.first().batteryTemperatureCelsius.toFloat(), temperatureUnit)) + temperatureUnit.shortRepresentation
        }
        var auxCount = 0
        val auxModesString = run {
            val list = mutableListOf<String>()

            for(device in getOrderedValues(info.deviceMap)) {
                val string = when(device){
                    is FXStatusPacket -> {
                        val auxString = if (MiscMode.AUX_OUTPUT_ON.isActive(device.miscValue)) {
                            auxCount++
                            "ON"
                        } else {
                            "Off"
                        }
                        oneWord(getDeviceString(info, device) + auxString)
                    }
                    is MXStatusPacket -> {
                        val auxActive = AuxMode.isAuxModeActive(device.rawAuxModeValue)
                        val auxDisabled = AuxMode.DISABLED.isActive(device.auxModeValue)
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

        val fxACModesString = if(info.fxMap.values.map { it.acMode }.toSet().size > 1){
            getOrderedValues(info.fxMap).joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.acModeName}" }
        } else {
            info.acMode.modeName
        }

        val dailyKWHString = getOrderedIdentifiers(dailyInfo.dailyKWHMap.keys).joinToString(SEPARATOR) {
            val dailyKWH = dailyInfo.dailyKWHMap[it] ?: error("No dailyKWH value for $it")
            val device = info.deviceMap[it] ?: error("No device in device map for ${it.fragmentId} ${it.identifier.representation}")
            getDeviceString(info, device as DocumentedPacket<*>) + Formatting.FORMAT.format(dailyKWH)
        }
        val modesString = getOrderedValues(info.deviceMap).joinToString(SEPARATOR) { "${getDeviceString(info, it)}${getModeName(it)}" }

        val pvWattagesString = getOrderedValues(info.basicChargeControllerMap).joinToString(SEPARATOR) {
            "${getDeviceString(info, it as SolarStatusPacket)}${wattsToKilowattsString(it.pvCurrent.toDouble() * it.inputVoltage.toDouble())}"
        }
        val chargerWattagesString = getOrderedValues(info.basicChargeControllerMap).joinToString(SEPARATOR) {
            "${getDeviceString(info, it as SolarStatusPacket)}${wattsToKilowattsString(it.chargingPower)}"
        }

        val fxWarningsString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.warningsString}" }
        val fxErrorsString = info.fxMap.values.joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.errorsString}" }
        val mxErrorsString = info.mxMap.values.joinToString(SEPARATOR) { "${getDeviceString(info, it)}${it.errorsString}" }
        val roverErrorsString = info.roverMap.values.joinToString(SEPARATOR) {
            "${getDeviceString(info, it)}${Modes.toString(RoverErrorMode::class.java, it.errorModeValue)}"
        }
        val batteryVoltagesString = run {
            val map = LinkedHashMap<String, MutableList<String>>() // maintain insertion order
            for(device in getOrderedValues(info.batteryMap)){
                val batteryVoltageString = Formatting.TENTHS.format(device.batteryVoltage)
                map.getOrPut(batteryVoltageString, ::mutableListOf).add(getDeviceString(info, device as SolarStatusPacket, includeParenthesis = false))
            }
            map.entries.joinToString(SEPARATOR) { (batteryVoltageString, deviceList) ->
                "(" + deviceList.joinToString(",") + ")" + batteryVoltageString
            } + "$DOUBLE_SEPARATOR${info.estimatedBatteryVoltageString} V"
        }
        val acModeString = if(info.fxMap.isNotEmpty()) { "$SEPARATOR$fxACModesString" } else ""

        val dailyFXInfo = dailyInfo.dailyFXInfo
        val dailyFXLine = if(dailyFXInfo == null) "" else {
            "FX: Discharge: <strong>${Formatting.TENTHS.format(dailyFXInfo.inverterKWH)}</strong> kWh | " +
                    "Charge: <strong>${Formatting.TENTHS.format(dailyFXInfo.chargerKWH)}</strong> kWh\n"
        }
        val basicChargeControllerString = if(info.basicChargeControllerMap.size > 1) {
            "PV: $pvWattagesString | Total: <strong>${wattsToKilowattsString(info.pvWattage)}</strong> kW\n" +
                    "Charger: $chargerWattagesString | " + oneWord("Total: <strong>${wattsToKilowattsString(info.pvChargerWattage)}</strong> kW") + "\n"
        } else {
            "PV: <strong>${wattsToKilowattsString(info.pvWattage)}</strong> kW | " +
                    "Charger: <strong>${wattsToKilowattsString(info.pvChargerWattage)}</strong> kW\n"
        }
        val dailyChargeControllerString = when(dailyInfo.dailyKWHMap.size) {
            0 -> ""
            1 -> {
                "Daily kWh: <strong>${dailyInfo.dailyKWHString}</strong>\n"
            }
            else -> {
                "Daily kWh: $dailyKWHString | " + oneWord("Total: <strong>${dailyInfo.dailyKWHString}</strong>") + "\n"
            }
        }
        val deviceCpuTemperatureString = if(info.deviceCpuTemperatureMap.isEmpty()) "" else ("CPU: " + info.deviceCpuTemperatureMap.map { (fragmentId, cpuTemperaturePacket) ->
            (fragmentId?.toString() ?: "~") + ": " + Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(cpuTemperaturePacket, temperatureUnit)) + temperatureUnit.shortRepresentation
        }.joinToString(SEPARATOR) + "\n")

        val text = "" +
                basicChargeControllerString +
                dailyChargeControllerString +
                dailyFXLine +
                "$devicesString$batteryTemperatureString$acModeString\n" +
                (if(info.fxMap.values.any { it.errorModeValue != 0 }) "FX Errors: $fxErrorsString\n" else "") +
                (if(info.mxMap.values.any { it.errorModeValue != 0 }) "MX Errors: $mxErrorsString\n" else "") +
                (if(info.roverMap.values.any { it.errorModes.isNotEmpty() }) "Rover Errors: $roverErrorsString\n" else "") +
                (if(info.fxMap.values.any { it.warningModeValue != 0 }) "FX Warn: $fxWarningsString\n" else "") +
                "Mode: $modesString\n" +
                (if(info.batteryMap.size > 1) "Batt: $batteryVoltagesString\n" else "") +
                deviceCpuTemperatureString +
                "Aux: $auxModesString"
        if(text.length > 5 * 1024){
            System.err.println("bigText.length: ${text.length}! Some text may be cut off")
        }

        val style = Notification.BigTextStyle().bigText(fromHtml(text))

        val isOld = System.currentTimeMillis() - info.dateMillis > 7 * 60 * 1000 // more than 7 minutes old
        val builder = createNotificationBuilder(context, NotificationChannels.SOLAR_STATUS.id, SOLAR_NOTIFICATION_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.solar_panel)
            .setSubText(summary)
            .setContentTitle((if(isOld) "!" else "") + "Batt: ${info.batteryVoltageString} V" + (if(info.fxMap.isNotEmpty()) " Load: ${wattsToKilowattsString(info.load)} kW" else ""))
            .setContentText("pv:${wattsToKilowattsString(info.pvWattage)} kWh:${dailyInfo.dailyKWHString} err:${info.errorsCount}" + (if(info.hasWarnings) " warn:${info.warningsCount}" else "") +  (if(auxCount > 0) " aux:$auxCount" else "") + " generator:" + if(info.acMode != ACMode.NO_AC) "ON" else "off")
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
    @Deprecated("Use createMXDayEnd()")
    fun createEndOfDay(context: Context, currentInfo: SolarPacketInfo, dailyChargeController: DailyChargeController, dateMillis: Long): Pair<Notification, Notification> {
        val builder = createNotificationBuilder(context, NotificationChannels.END_OF_DAY.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setContentTitle(when(dailyChargeController){
                is MXStatusPacket -> "MX on port ${dailyChargeController.address} end of day"
                is RoverStatusPacket -> "Rover ${currentInfo.getRoverId(dailyChargeController)} ${dailyChargeController.productSerialNumber} end of day"
                else -> error("dailyChargeController: $dailyChargeController is not supported!")
            })
            .setContentText("Got ${Formatting.FORMAT.format(dailyChargeController.dailyKWH)} kWh")
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

    fun createMXDayEnd(context: Context, packet: MXDayEndPacket, dateMillis: Long): Pair<Notification, Notification> {
        val builder = createNotificationBuilder(context, NotificationChannels.END_OF_DAY.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setContentTitle("MX on port ${packet.address} end of day")
            .setContentText("Got ${Formatting.FORMAT.format(packet.dailyKWH)} kWh")
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
    fun createFXDayEnd(context: Context, packet: FXDayEndPacket, dateMillis: Long): Pair<Notification, Notification> {
        val builder = createNotificationBuilder(context, NotificationChannels.END_OF_DAY.id, null)
            .setSmallIcon(R.drawable.solar_panel)
            .setWhen(dateMillis)
            .setShowWhen(true)
            .setContentTitle("FX on port ${packet.address} end of day")
            .setContentText("(kWh): Discharged: ${Formatting.TENTHS.format(packet.inverterKWH)} | Charged: ${Formatting.TENTHS.format(packet.chargerKWH)} | Bought: ${Formatting.TENTHS.format(packet.buyKWH)} ")
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
    fun createDeviceConnectionStatus(context: Context, device: SolarStatusPacket, justConnected: Boolean, dateMillis: Long): Pair<Notification, Notification>{
        val name = when(device.packetType){
            SolarStatusPacketType.FX_STATUS -> "FX"
            SolarStatusPacketType.MXFM_STATUS -> "MX"
            SolarStatusPacketType.RENOGY_ROVER_STATUS -> "Rover"
            else -> device.packetType.toString()
        }
        val deviceName = when(device){
            is OutbackData -> "$name on port ${device.address}"
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
    fun createMoreInfoNotification(context: Context, device: SolarStatusPacket, dateMillis: Long, packetInfo: SolarPacketInfo, moreRoverInfoAction: String? = null): Pair<Notification, Notification?>{
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
        val fragmentId = packetInfo.packetGroup.getFragmentId(device)
        when(device){
            is OutbackData -> {
                when(device.packetType){
                    SolarStatusPacketType.FX_STATUS -> {
                        device as FXStatusPacket
                        val dailyFX = packetInfo.dailyFXMap[IdentifierFragment.create(fragmentId, device.identifier)]
                        builder.setContentTitle("FX on port ${device.address}")
                        val batteryVoltage = Formatting.TENTHS.format(device.batteryVoltage)
                        builder.setSubText("${batteryVoltage}V | ${device.operatingModeName} | Inv: ${Formatting.OPTIONAL_TENTHS.format(device.inverterCurrent)}A | ${getTimeString(dateMillis)}")
                        builder.style = Notification.BigTextStyle().bigText(
                            "Battery: $batteryVoltage V | ${device.operatingModeName} | ${device.acModeName}\n" +
                                    "AC Output: ${device.outputVoltage} V | AC Input: ${device.inputVoltage} V\n" +
                                    "Inverter Current: ${Formatting.OPTIONAL_TENTHS.format(device.inverterCurrent)} | Sell Current: ${Formatting.OPTIONAL_TENTHS.format(device.sellCurrent)}\n" +
                                    "Buy Current: ${Formatting.OPTIONAL_TENTHS.format(device.buyCurrent)} | Charger Current: ${Formatting.OPTIONAL_TENTHS.format(device.chargerCurrent)}\n" +
                                    "230V: " + (if(MiscMode.FX_230V_UNIT.isActive(device.miscValue)) "yes" else "no") + " | " +
                                    "Aux: " + (if(MiscMode.AUX_OUTPUT_ON.isActive(device.miscValue)) "on" else "off") + " | " +
                                    "Errors: ${device.errorsString} | Warnings: ${device.warningsString} " +
                                    if(dailyFX == null) "" else ("\n" +
                                            "Battery Min: ${Formatting.TENTHS.format(dailyFX.dailyMinBatteryVoltage)} V Max: ${Formatting.TENTHS.format(dailyFX.dailyMaxBatteryVoltage)} V\n" +
                                            "Inverter: ${Formatting.HUNDREDTHS.format(dailyFX.inverterKWH)} kWh\n" +
                                            "Buy: ${Formatting.HUNDREDTHS.format(dailyFX.buyKWH)} kWh | Sell: ${Formatting.HUNDREDTHS.format(dailyFX.sellKWH)} kWh\n" +
                                            "Charge: ${Formatting.HUNDREDTHS.format(dailyFX.chargerKWH)} kWh\n")
                        )
                    }
                    SolarStatusPacketType.MXFM_STATUS -> {
                        device as MXStatusPacket
                        builder.setContentTitle("MX on port ${device.address}")
                        val batteryVoltage = Formatting.TENTHS.format(device.batteryVoltage)
                        builder.setSubText("$batteryVoltage | ${device.chargerModeName} | ${device.chargingCurrent}A | ${device.dailyKWH} kWh | ${getTimeString(dateMillis)}")
                        builder.style = Notification.BigTextStyle().bigText(
                                "Battery Voltage: $batteryVoltage V\n" +
                                    createChargeControllerMoreInfo(device) +
                                    "Charger Mode: ${device.chargerModeName}\n" +
                                    "Aux Mode: ${device.auxModeName} | FM Aux On: " + (if(AuxMode.isAuxModeActive(device.rawAuxModeValue)) "yes" else "no") + "\n" +
                                    "Errors: ${device.errorsString}\n" +
                                    "Daily kWh: ${device.dailyKWH} | " +
                                    "Daily Ah: ${device.dailyAH} " + when(device.dailyAHSupport!!) {
                                        Support.FULLY_SUPPORTED -> ""
                                        Support.NOT_SUPPORTED -> "(Not Supported)"
                                        Support.UNKNOWN -> "(Maybe supported)"
                                    }
                        )
                    }
                    else -> throw IllegalArgumentException("$device not supported!")
                }
            }
            is RoverStatusPacket -> {
                builder.setContentTitle("Rover with serial: ${device.productSerialNumber}")
                val batteryVoltage = Formatting.TENTHS.format(device.batteryVoltage)
                builder.setSubText("$batteryVoltage | ${device.chargingMode.modeName} | ${device.chargingCurrent}A | ${device.batteryTemperatureCelsius}C | ${device.dailyKWH} kWh | ${getTimeString(dateMillis)}")
                builder.style = Notification.BigTextStyle().bigText(
                        "Battery Voltage: $batteryVoltage | SOC: ${device.batteryCapacitySOC}% | ${device.recognizedVoltage?.modeName ?: "??V"}/${device.systemVoltageSetting.modeName}\n" +
                        createChargeControllerMoreInfo(device) +
                        "Charging State: ${device.chargingMode.modeName} | Load Mode: ${device.loadWorkingModeValue}\n" +
                        "Errors: ${Modes.toString(RoverErrorMode::class.java, device.errorModeValue)}\n" +
                        "Temperature: Controller: ${device.controllerTemperatureCelsius}C | Battery: ${device.batteryTemperatureCelsius}C\n" +
                        "Day: ${device.operatingDaysCount} | kWh: ${device.dailyKWH} | Ah: ${device.dailyAH} | " +
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
    private fun createChargeControllerMoreInfo(device: BasicChargeController): String{
        return "Charging: Current: ${Formatting.TENTHS.format(device.chargingCurrent)}A | " +
                "Power: ${Formatting.TENTHS.format(device.chargingPower)}W\n" +
                "PV: ${Formatting.OPTIONAL_TENTHS.format(device.pvCurrent)}A * Voltage: ${Formatting.OPTIONAL_TENTHS.format(device.inputVoltage)}V = " +
                "${Formatting.OPTIONAL_TENTHS.format(device.inputVoltage.toDouble() * device.pvCurrent.toDouble())}W\n"
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
                "Cumulative: kWh: ${device.cumulativeKWH} Ah: ${device.chargingAmpHoursOfBatteryCount}\n" +
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
    fun createTemperatureNotification(context: Context, dateMillis: Long, temperatureName: String, deviceName: String, temperatureCelsius: Float, over: Boolean, critical: Boolean, temperatureUnit: TemperatureUnit): Notification {
        val builder = createNotificationBuilder(context, NotificationChannels.TEMPERATURE_NOTIFICATION.id, null)
            .setSmallIcon(R.drawable.power_button)
            .setShowWhen(true)
            .setWhen(dateMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(critical){
                builder.setColorized(true)
            }
        }
        val temperatureString = "${Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(temperatureCelsius, temperatureUnit))}${temperatureUnit.shortRepresentation}"
        if(over){
            val start = if(critical) "$temperatureName is HIGH! " else "$temperatureName is high!"
            builder.setContentTitle("$start $temperatureString")
        } else {
            val start = if(critical) "$temperatureName is LOW! " else "$temperatureName is low!"
            builder.setContentTitle("$start $temperatureString")
        }
        builder.setContentText("From $deviceName")

        val r = builder.build()
        if(critical) {
            r.flags = r.flags or Notification.FLAG_INSISTENT
        }
        return r
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
