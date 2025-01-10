package me.retrodaredevil.solarthing.android.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import me.retrodaredevil.solarthing.android.createConnectionProfileManager
import me.retrodaredevil.solarthing.android.getSavedKeyPair
import me.retrodaredevil.solarthing.android.getSenderName
import me.retrodaredevil.solarthing.android.notifications.CANCEL_SCHEDULED_COMMAND_NO_GENERATED_KEY_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.CANCEL_SCHEDULED_COMMAND_RESULT_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.CANCEL_SCHEDULED_COMMAND_SERVICE_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.prefs.CouchDbDatabaseConnectionProfile
import me.retrodaredevil.solarthing.android.util.ServiceHelper
import me.retrodaredevil.solarthing.android.util.createCouchDbInstance
import me.retrodaredevil.solarthing.commands.packets.open.ImmutableDeleteAlterPacket
import me.retrodaredevil.solarthing.commands.util.CommandManager
import me.retrodaredevil.solarthing.database.SolarThingDatabase
import me.retrodaredevil.solarthing.database.couchdb.CouchDbSolarThingDatabase
import me.retrodaredevil.solarthing.database.couchdb.RevisionUpdateToken
import me.retrodaredevil.solarthing.database.exception.SolarThingDatabaseException
import me.retrodaredevil.solarthing.packets.collection.PacketCollectionIdGenerator
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private class State {
    val executorService: ExecutorService = Executors.newFixedThreadPool(1)
}

/**
 * A service used for sending commands. Only one of these services can be active at a time,
 * which means that only one command can be sent using this at once.
 */
class CancelCommandService : Service() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CancelCommandService::class.java)
        val serviceHelper = ServiceHelper(CancelCommandService::class.java)
    }

    private lateinit var state: State

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getDatabase(): SolarThingDatabase {
        val connectionProfileManager = createConnectionProfileManager(this)
        val activeConnectionProfile = connectionProfileManager.activeProfile.profile
        val couchDbDatabaseConnectionProfile = (activeConnectionProfile.databaseConnectionProfile as CouchDbDatabaseConnectionProfile)
        val properties = couchDbDatabaseConnectionProfile.createCouchProperties()
        val instance = createCouchDbInstance(properties)
        return CouchDbSolarThingDatabase.create(instance)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent!!
        state = State()
        val documentId = intent.getStringExtra("documentId") ?: error("documentId was null!")
        val revision = intent.getStringExtra("revision") ?: error("revision was null!")
        val sourceId = intent.getStringExtra("sourceId") ?: error("sourceId was null!")

        val database = getDatabase()
        val keyPair = getSavedKeyPair(this)
        if (keyPair == null){
            val notificationId = CANCEL_SCHEDULED_COMMAND_NO_GENERATED_KEY_NOTIFICATION_ID
            getManager().notify(notificationId, NotificationHandler.createNoGeneratedKeyNotification(this, notificationId))
            stopSelf()
            return START_NOT_STICKY // TODO is there a way we can return something without actually "starting" the service?
        }
        val sender = getSenderName(this)

        val commandManager = CommandManager({ keyPair }, sender) // keyPair may not be initialized now, but that's OK
        val creator = commandManager.makeCreator(sourceId, ZoneId.systemDefault(), null, ImmutableDeleteAlterPacket(documentId, RevisionUpdateToken(revision)), PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR)
        getManager().cancel(CANCEL_SCHEDULED_COMMAND_RESULT_NOTIFICATION_ID)
        val cancellingCommandNotification = NotificationHandler.createCancellingCommandNotification(this, Instant.now(), CANCEL_SCHEDULED_COMMAND_SERVICE_NOTIFICATION_ID, documentId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Q is SDK 19
            // WE have access to this on SDK 29, and using it is required when targeting SDK 34 and above
            // https://developer.android.com/about/versions/14/changes/fgs-types-required#data-sync
            startForeground(CANCEL_SCHEDULED_COMMAND_SERVICE_NOTIFICATION_ID, cancellingCommandNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(CANCEL_SCHEDULED_COMMAND_SERVICE_NOTIFICATION_ID, cancellingCommandNotification)
        }
        state.executorService.submit {
            val now = Instant.now()
            val packetCollection = creator.create(now)
            var success = false
            try {
                database.openDatabase.uploadPacketCollection(packetCollection, null)
                success = true
            } catch (ex: SolarThingDatabaseException) {
                LOGGER.error("Could not upload request to delete alter packet. A notification will be sent to inform the user of this.", ex)
            }

            val notificationId = CANCEL_SCHEDULED_COMMAND_RESULT_NOTIFICATION_ID
            getManager().notify(notificationId, NotificationHandler.createCancelCommandResultNotification(this, success, notificationId, documentId))
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        state.executorService.shutdownNow()
        super.onDestroy()
    }
}
