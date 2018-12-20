package me.retrodaredevil.solarthing.android

import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun millisToString(millis: Long): String{
    val absTimeLeft = abs(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(absTimeLeft)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(absTimeLeft) - TimeUnit.HOURS.toMinutes(hours)
    val minutesString = minutes.toString()

    return "$hours" +
            ":" +
            (if(minutesString.length == 1) "0$minutesString" else minutesString) +
            " " +
            if(millis < 0) "PAST" else "left"
}