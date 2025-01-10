package me.retrodaredevil.solarthing.android.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarThingApplication
import me.retrodaredevil.solarthing.android.activity.MainActivity
import me.retrodaredevil.solarthing.android.createConnectionProfileManager
import me.retrodaredevil.solarthing.android.createMiscProfileProvider
import me.retrodaredevil.solarthing.android.createSolarProfileManager
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.notifications.PERSISTENT_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.getGroup
import me.retrodaredevil.solarthing.android.prefs.ConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.CouchDbDatabaseConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.MiscProfile
import me.retrodaredevil.solarthing.android.prefs.NetworkSwitchingProfile
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import me.retrodaredevil.solarthing.android.prefs.ProfileProvider
import me.retrodaredevil.solarthing.android.prefs.SolarProfile
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.request.DataRequester
import me.retrodaredevil.solarthing.android.request.DataRequesterMultiplexer
import me.retrodaredevil.solarthing.android.request.MillisDatabaseDataRequester
import me.retrodaredevil.solarthing.android.util.SSIDNotAvailable
import me.retrodaredevil.solarthing.android.util.SSIDPermissionException
import me.retrodaredevil.solarthing.android.util.ServiceHelper
import me.retrodaredevil.solarthing.android.util.createCouchDbInstance
import me.retrodaredevil.solarthing.android.util.createExplicitIntent
import me.retrodaredevil.solarthing.android.util.getSSID
import me.retrodaredevil.solarthing.android.util.registerReceiverNotExported
import me.retrodaredevil.solarthing.database.MillisDatabase
import me.retrodaredevil.solarthing.database.SolarThingDatabase
import me.retrodaredevil.solarthing.database.couchdb.CouchDbSolarThingDatabase
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future


private const val STOP_SERVICE_ACTION = "me.retrodaredevil.solarthing.android.service.action.stop_service"
private const val RELOAD_SERVICE_ACTION = "me.retrodaredevil.solarthing.android.service.action.reload_service"
private const val RESTART_SERVICE_ACTION = "me.retrodaredevil.solarthing.android.service.action.restart_service"

private class MillisServiceObject(
    val millisDataService: MillisDataService,
    val millisDatabaseGetter: (SolarThingDatabase) -> MillisDatabase
){
    var future: Future<*>? = null
}


class PersistentService : Service(), Runnable{
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PersistentService::class.java)
        val serviceHelper = ServiceHelper(PersistentService::class.java)
    }
    private var initialized = false
    private lateinit var handler: Handler
    private lateinit var connectionProfileManager: ProfileManager<ConnectionProfile>
    private lateinit var solarProfileManager: ProfileManager<SolarProfile>
    private lateinit var miscProfileProvider: ProfileProvider<MiscProfile>

    private lateinit var executorService: ExecutorService
    private lateinit var millisServices: List<MillisServiceObject>

    private var metaUpdaterFuture: Future<*>? = null
    private lateinit var metaHandler: MetaHandler
    private var alterUpdaterFuture: Future<*>? = null
    private lateinit var alterHandler: AlterHandler
    private lateinit var alterService: AlterService

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ShowToast")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler = Handler(Looper.getMainLooper())
        connectionProfileManager = createConnectionProfileManager(this)
        solarProfileManager = createSolarProfileManager(this)
        miscProfileProvider = createMiscProfileProvider(this)

        executorService = Executors.newCachedThreadPool()
        metaHandler = MetaHandler()
        alterHandler = AlterHandler()
        alterService = AlterService(this)

        val solarStatusData = PacketGroupData()
        val solarEventData = PacketGroupData()
        val application = application as SolarThingApplication
        application.solarStatusData = solarStatusData
        application.solarEventData = solarEventData
        application.metaHandler = metaHandler
        millisServices = listOf(
                MillisServiceObject(
                        SolarStatusService(this, connectionProfileManager, solarProfileManager, createMiscProfileProvider(this), solarStatusData, solarEventData, metaHandler),
                        SolarThingDatabase::getStatusDatabase
                ),
                MillisServiceObject(
                        SolarEventService(this, solarEventData),
                        SolarThingDatabase::getEventDatabase
                )
        )
        for(service in millisServices){
            service.millisDataService.onInit()
        }
        alterService.register()
        handler.postDelayed(this, 300)
        Toast.makeText(this, "SolarThing Notification Service started", Toast.LENGTH_LONG).show()
        LOGGER.info("Starting service")
        updateNotification(System.currentTimeMillis() + 300)
        initialized = true
        return START_STICKY
    }
    private fun updateNotification(countDownWhen: Long){
        // unanswered question with problem we're having here: https://stackoverflow.com/questions/47703216/android-clicking-grouped-notifications-restarts-app
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val builder = getBuilder()
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.sun)
                .setContentTitle("SolarThing service is running")
                .setContentText("${millisServices.count { it.millisDataService.shouldUpdate }} service(s) are running")
                .setContentIntent(PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE))
                .setWhen(1) // make it the lowest priority
                .setShowWhen(false)
                .setGroup(getGroup(PERSISTENT_NOTIFICATION_ID))
                .setCategory(Notification.CATEGORY_SERVICE)

        // countdown
        builder.setUsesChronometer(true)
        builder.setChronometerCountDown(true)
        builder.setWhen(countDownWhen)
        // buttons
        builder.addAction(
                Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.sun),
                         "Stop",
                        PendingIntent.getBroadcast(
                                 this, 0,
                                createExplicitIntent(this, STOP_SERVICE_ACTION),
                                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                ).build()
        )
        builder.addAction(
                Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.sun),
                        "Reload",
                        PendingIntent.getBroadcast(
                                this, 0,
                                createExplicitIntent(this, RELOAD_SERVICE_ACTION),
                                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                ).build()
        )
        builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.sun),
                    "Restart",
                    PendingIntent.getBroadcast(
                            this, 0,
                            createExplicitIntent(this, RESTART_SERVICE_ACTION),
                            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
        )
        val intentFilter = IntentFilter()
        intentFilter.addAction(STOP_SERVICE_ACTION)
        intentFilter.addAction(RELOAD_SERVICE_ACTION)
        intentFilter.addAction(RESTART_SERVICE_ACTION)
        registerReceiverNotExported(this, receiver, intentFilter)
        val notification = builder.build()
        getManager().notify(PERSISTENT_NOTIFICATION_ID, notification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Q is SDK 29
            // WE have access to this on SDK 29, and using it is required when targeting SDK 34 and above
            // https://developer.android.com/about/versions/14/changes/fgs-types-required#data-sync
            // TODO we might have to request foreground service type LOCATION here so that SSID switching works
            startForeground(PERSISTENT_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            // TODO should we use ServiceCompat.startForeground()?
            startForeground(PERSISTENT_NOTIFICATION_ID, notification)
        }
    }
    private fun getBuilder(): Notification.Builder {
        return Notification.Builder(this, NotificationChannels.PERSISTENT.id)
    }
    private fun getManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun <T> getProfileToSwitchTo(ssid: String?, manager: ProfileManager<T>, networkSwitchingProfileGetter: (T) -> NetworkSwitchingProfile): UUID? {
        var r: UUID? = null
        for(uuid in manager.profileUUIDs){
            val networkSwitchingProfile = networkSwitchingProfileGetter(manager.getProfile(uuid).profile)
            if(networkSwitchingProfile.isEnabled){
                if(networkSwitchingProfile.isBackup){
                    if(r == null){
                        r = uuid
                    }
                } else if(networkSwitchingProfile.ssid == ssid){
                    return uuid
                }
            }
        }
        return r
    }

    /*
    This is called periodically whenever we need to update
     */
    override fun run() {
        if(miscProfileProvider.activeProfile.profile.networkSwitchingEnabled) {
            try {
                val id = getSSID(this)
                LOGGER.debug("Current ssid: $id")
                val switchUUID = getProfileToSwitchTo(id, connectionProfileManager, ConnectionProfile::networkSwitchingProfile)
                if (switchUUID != null && connectionProfileManager.activeUUID != switchUUID) {
                    connectionProfileManager.activeUUID = switchUUID
                    Toast.makeText(this, "Changed to profile: ${connectionProfileManager.getProfileName(switchUUID)}", Toast.LENGTH_SHORT).show()
                }
            } catch(ex: SSIDPermissionException){
                LOGGER.info("Got permission exception while trying to get SSID", ex)
            } catch(ex: SSIDNotAvailable){
                LOGGER.info("Got unknown SSID", ex)
            }
        }

        val activeConnectionProfile = connectionProfileManager.activeProfile.profile

        val couchDbDatabaseConnectionProfile = (activeConnectionProfile.databaseConnectionProfile as CouchDbDatabaseConnectionProfile)
        val properties = couchDbDatabaseConnectionProfile.createCouchProperties()
        val instance = createCouchDbInstance(properties)
        val database = CouchDbSolarThingDatabase.create(instance)
        var needsLargeData = false
        for(service in millisServices){
            val future = service.future
            if(future != null){
                service.future = null
                if(future.cancel(true)) { // if the task was still running then...
                    service.millisDataService.onTimeout()
                }
            }
            if(!service.millisDataService.shouldUpdate){
                service.millisDataService.onCancel()
                continue
            }

            val dataRequesters = listOf(
                    MillisDatabaseDataRequester(
                            service.millisDatabaseGetter(database),
                            properties.host,
                            service.millisDataService::recommendedMillisQuery // HERE
                    )
            )
            if(service.millisDataService.updatePeriodType == UpdatePeriodType.LARGE_DATA){
                needsLargeData = true
            }
            val dataRequester = DataRequesterMultiplexer(dataRequesters)
            service.future = submit(DataUpdater(dataRequester, service.millisDataService::onDataRequest))
        }
        metaUpdaterFuture?.cancel(true)
        metaUpdaterFuture = submit(MetaUpdater(database, metaHandler))

        val preferredSourceId = createConnectionProfileManager(this).activeProfile.profile.preferredSourceId
        alterUpdaterFuture?.cancel(true)
        if (preferredSourceId != null) {
            alterUpdaterFuture = submit(AlterUpdater(database, alterHandler, preferredSourceId, this))
        } else {
            LOGGER.warn("Not updating alter packets because the user has not set a preferred source ID")
        }

        val delay = if(needsLargeData){ activeConnectionProfile.initialRequestTimeSeconds * 1000L } else { activeConnectionProfile.subsequentRequestTimeSeconds * 1000L }
        handler.postDelayed(this, delay)
        updateNotification(System.currentTimeMillis() + delay)
    }
    private fun submit(runnable: Runnable): Future<*> {
        return executorService.submit {
            try {
                runnable.run()
            } catch (th: Throwable) {
                LOGGER.error("Got error from a submitted Runnable", th)
                throw th
            }
        }
    }
    private fun reload(){
        handler.removeCallbacks(this)
        handler.postDelayed(this, 100)
    }

    override fun onDestroy() {
        if(!initialized){
            LOGGER.info("This PersistentService wasn't initialized for some reason... Not to worry, we prepared for this!")
            return
        }
        LOGGER.info("Stopping persistent service")
        handler.removeCallbacks(this)
        unregisterReceiver(receiver)
        for(service in millisServices){
            service.future?.cancel(true)
            service.millisDataService.onCancel()
            service.millisDataService.onEnd()
        }
        metaUpdaterFuture?.cancel(true)
        metaUpdaterFuture = null

        alterUpdaterFuture?.cancel(true)
        alterUpdaterFuture = null
        cancelAlterNotifications(this)
        alterService.unregister()

        executorService.shutdownNow()
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(context != null && intent != null){
                when(intent.action){
                    STOP_SERVICE_ACTION -> serviceHelper.stopService(context)
                    RELOAD_SERVICE_ACTION -> reload()
                    RESTART_SERVICE_ACTION -> serviceHelper.restartService(context)
                    else -> LOGGER.warn("unknown action: ${intent.action}")
                }
            }
        }

    }
}
private class DataUpdater(
        private val dataRequester: DataRequester,
        private val updateNotification: (dataRequest: DataRequest) -> Unit
) : Runnable {
    override fun run() {
        val result = dataRequester.requestData()
        updateNotification(result)
    }
}


