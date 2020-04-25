package me.retrodaredevil.solarthing.android.prefs

class VoltageTimerNode(
    val timeMinutes: Float,
    val isHighVoltageTimer: Boolean,
    /** The battery voltage when the timer starts counting down (inclusive)*/
    val batteryVoltageStart: Float,
    /** The battery voltage when the timer resets (exclusive)*/
    val batteryVoltageReset: Float?,
    /** The battery voltage when the timer pauses (exclusive)*/
    val batteryVoltagePause: Float?,
    /** The battery voltage when the timer starts counting up(exclusive)*/
    val batteryVoltageCountUp: Float?,
    /** If true, when the timer is not at maximum, a persistent notification will be shown */
    val persistentNotification: Boolean,
    /** The battery voltage to use */
    val preferredBatteryVoltageType: BatteryVoltageType?
)
