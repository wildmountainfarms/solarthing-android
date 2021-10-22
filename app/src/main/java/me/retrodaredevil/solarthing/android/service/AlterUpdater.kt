package me.retrodaredevil.solarthing.android.service

import android.content.Context
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.notifications.SCHEDULED_COMMAND_MAX_NOTIFICATIONS
import me.retrodaredevil.solarthing.android.notifications.SCHEDULED_COMMAND_NOTIFICATION_START_ID
import me.retrodaredevil.solarthing.database.SolarThingDatabase
import me.retrodaredevil.solarthing.database.VersionedPacket
import me.retrodaredevil.solarthing.database.exception.SolarThingDatabaseException
import me.retrodaredevil.solarthing.type.alter.StoredAlterPacket
import me.retrodaredevil.solarthing.type.alter.packets.ScheduledCommandPacket
import java.lang.RuntimeException

fun cancelAlterNotifications(context: Context) {
    context.getManager().apply {
        for (notificationId in SCHEDULED_COMMAND_NOTIFICATION_START_ID until (SCHEDULED_COMMAND_NOTIFICATION_START_ID + SCHEDULED_COMMAND_MAX_NOTIFICATIONS)) {
            cancel(notificationId)
        }
    }
}
class AlterUpdater(
        private val database: SolarThingDatabase,
        private val alterHandler: AlterHandler,
        private val sourceId: String,
        private val context: Context,
) : Runnable {
    override fun run() {
        println("Going to get alter packets")
        try {
            val packets: List<VersionedPacket<StoredAlterPacket>> = database.alterDatabase.queryAll(sourceId)
            println("Got ${packets.size} alter packets!")
            alterHandler.update(packets)
            try {
                updateNotifications(packets)
            } catch(ex: Exception) {
                // This is here because I need to know when this fails, and currently executor services don't print the stacktrace when they swallow an exception
                ex.printStackTrace()
            }
        } catch (ex: SolarThingDatabaseException) {
            throw RuntimeException(ex)
        }
    }
    private fun updateNotifications(packets: List<VersionedPacket<StoredAlterPacket>>) {
        val scheduledStoredPackets: List<Pair<VersionedPacket<StoredAlterPacket>, ScheduledCommandPacket>> = packets.mapNotNull {
            val packet = it.packet.packet
            if (packet is ScheduledCommandPacket) Pair(it, packet) else null
        }
                .sortedBy { it.second.data.scheduledTimeMillis }
                .take(5) // maximum of 5 notifications for now
        println("Going to send ${scheduledStoredPackets.size} scheduled command notifications")
        val manager = context.getManager()
        println("here1 $scheduledStoredPackets")
        for ((i, pair) in scheduledStoredPackets.withIndex()) {
            val notificationId = SCHEDULED_COMMAND_NOTIFICATION_START_ID + i
            val (versionedPacket, scheduledCommandPacket) = pair
            val notification = NotificationHandler.createScheduledCommandNotification(context, versionedPacket, scheduledCommandPacket)
            manager.notify(notificationId, notification)
            println("Notifying with $notificationId")
        }
        println("here2")
        for (notificationId in (SCHEDULED_COMMAND_NOTIFICATION_START_ID + scheduledStoredPackets.size) until (SCHEDULED_COMMAND_NOTIFICATION_START_ID + SCHEDULED_COMMAND_MAX_NOTIFICATIONS)) {
            manager.cancel(notificationId)
            println("Cancelling $notificationId")
        }
        println("here3")
    }
}