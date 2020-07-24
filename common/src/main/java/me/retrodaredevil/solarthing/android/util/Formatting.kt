package me.retrodaredevil.solarthing.android.util

import java.text.DecimalFormat
import java.text.Format

object Formatting {
    val TENTHS: Format = DecimalFormat("0.0")
    val HUNDREDTHS: Format = DecimalFormat("0.00")
    val FORMAT: Format = DecimalFormat("0.0##")
    val OPTIONAL_HUNDRETHS: Format = DecimalFormat("0.0#")

    val OPTIONAL_TENTHS = DecimalFormat("0.#")
}

fun wattsToKilowattsString(watts: Number): String {
    return Formatting.HUNDREDTHS.format(watts.toDouble() / 1000.0)
}
