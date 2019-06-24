/**
 * To avoid notifications being grouped, these IDs can be used for the id and group of a notification
 */
package me.retrodaredevil.solarthing.android.notifications


const val PERSISTENT_NOTIFICATION_ID = 1
const val SOLAR_NOTIFICATION_ID = 2
const val GENERATOR_FLOAT_NOTIFICATION_ID = 3
const val GENERATOR_DONE_NOTIFICATION_ID = 4
const val BATTERY_NOTIFICATION_ID = 5
const val OUTHOUSE_NOTIFICATION_ID = 6
const val VACANT_NOTIFICATION_ID = 7

fun getGroup(id: Int) = "group$id"
