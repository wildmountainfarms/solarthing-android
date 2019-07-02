/**
 * To avoid notifications being grouped, these IDs can be used for the id and group of a notification
 */
package me.retrodaredevil.solarthing.android.notifications


const val PERSISTENT_NOTIFICATION_ID = 1
const val SOLAR_NOTIFICATION_ID = 2
const val GENERATOR_PERSISTENT_ID = 3
const val GENERATOR_FLOAT_NOTIFICATION_ID = 4
const val GENERATOR_DONE_NOTIFICATION_ID = 5
const val BATTERY_NOTIFICATION_ID = 6
const val OUTHOUSE_NOTIFICATION_ID = 7
const val VACANT_NOTIFICATION_ID = 8

const val MX_END_OF_DAY_SUMMARY_ID = 9
const val DEVICE_CONNECTION_STATUS_SUMMARY_ID = 10

const val MX_END_OF_DAY_GROUP = "mx"
const val DEVICE_CONNECTION_STATUS_GROUP = "connection"

fun getGroup(id: Int) = "group$id"

fun getMXEndOfDayInfoID(address: Int): Int{
    return 100 + address
}
fun getDeviceConnectionStatusID(address: Int): Int {
    return 200 + address
}
