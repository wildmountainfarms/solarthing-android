package me.retrodaredevil.solarthing.android

import java.text.DecimalFormat
import java.text.Format

object Formatting {
    val TENTHS: Format = DecimalFormat("0.0")
    val HUNDREDTHS: Format = DecimalFormat("0.00")
    val FORMAT: Format = DecimalFormat("0.0##")

    val OPTIONAL_TENTHS = DecimalFormat("0.#")
}
