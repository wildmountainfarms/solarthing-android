package me.retrodaredevil.solarthing.android

import android.content.Intent


fun Intent.getIntExtraOrNull(name: String): Int? {
    val value = getIntExtra(name, -1)
    if (value == -1) {
        val value2 = getIntExtra(name, 0)
        if (value2 == 0) {
            return null
        }
    }
    return value
}
