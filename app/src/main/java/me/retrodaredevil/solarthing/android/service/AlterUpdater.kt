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
import org.slf4j.LoggerFactory

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
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AlterUpdater::class.java)
        val CANCEL_COMMAND_ACTION = "me.retrodaredevil.solarthing.android.CANCEL_COMMAND_ACTION"
    }
    override fun run() {
        try {
            val packets: List<VersionedPacket<StoredAlterPacket>> = database.alterDatabase.queryAll(sourceId)
            alterHandler.update(packets)
            updateNotifications(packets)
        } catch (ex: SolarThingDatabaseException) {
            LOGGER.error("Error querying alter database", ex)
        }
    }
    private fun updateNotifications(packets: List<VersionedPacket<StoredAlterPacket>>) {
        val scheduledStoredPackets: List<Pair<VersionedPacket<StoredAlterPacket>, ScheduledCommandPacket>> = packets.mapNotNull {
            val packet = it.packet.packet
            if (packet is ScheduledCommandPacket) Pair(it, packet) else null
        }
                .sortedBy { it.second.data.scheduledTimeMillis }
                .take(5) // maximum of 5 notifications for now
        LOGGER.info("Going to send ${scheduledStoredPackets.size} scheduled command notifications")
        val manager = context.getManager()
        for ((i, pair) in scheduledStoredPackets.withIndex()) {
            val notificationId = SCHEDULED_COMMAND_NOTIFICATION_START_ID + i
            val (versionedPacket, scheduledCommandPacket) = pair
            val notification = NotificationHandler.createScheduledCommandNotification(context, versionedPacket, scheduledCommandPacket)
            manager.notify(notificationId, notification)
        }
        for (notificationId in (SCHEDULED_COMMAND_NOTIFICATION_START_ID + scheduledStoredPackets.size) until (SCHEDULED_COMMAND_NOTIFICATION_START_ID + SCHEDULED_COMMAND_MAX_NOTIFICATIONS)) {
            manager.cancel(notificationId)
        }
    }
}