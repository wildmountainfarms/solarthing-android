package me.retrodaredevil.solarthing.android.util

import java.text.DecimalFormat
import java.text.Format

object Formatting {
    val TENTHS: Format = DecimalFormat("0.0")
    val HUNDREDTHS: Format = DecimalFormat("0.00")
    val FORMAT: Format = DecimalFormat("0.0##")
    val OPTIONAL_HUNDREDTHS: Format = DecimalFormat("0.0#")
    val MINIMAL_HUNDREDTHS: Format = DecimalFormat("0.##")

    val OPTIONAL_TENTHS = DecimalFormat("0.#")
}

fun wattsToKilowattsString(watts: Number, format: Format = Formatting.HUNDREDTHS): String {
    return format.format(watts.toDouble() / 1000.0)
}
