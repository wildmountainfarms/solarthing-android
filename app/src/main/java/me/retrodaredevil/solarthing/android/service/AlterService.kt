package me.retrodaredevil.solarthing.android.service

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import me.retrodaredevil.solarthing.android.getIntExtraOrNull
import me.retrodaredevil.solarthing.android.notifications.CANCEL_SCHEDULED_COMMAND_ALREADY_RUNNING_NOTIFICATION_ID
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import org.slf4j.LoggerFactory

class AlterService(
        private val service: Service
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AlterService::class.java)
    }

    fun register() {
//        val handler = Handler(Looper.getMainLooper()) // This will cause the receiver to receive the event on a thread other than the main thread
//        service.registerReceiver(receiver, IntentFilter().apply { addAction(AlterUpdater.CANCEL_COMMAND_ACTION) }, null, handler)
        service.registerReceiver(receiver, IntentFilter().apply { addAction(AlterUpdater.CANCEL_COMMAND_ACTION) })
    }
    fun unregister() {
        service.unregisterReceiver(receiver)
    }

    private fun getKeyguardManager(): KeyguardManager {
        return service.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    private fun isScreenUnlocked(): Boolean {
        return !getKeyguardManager().isDeviceLocked
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context!!; intent!!
            when(intent.action){
                AlterUpdater.CANCEL_COMMAND_ACTION -> {
                    val documentId = intent.getStringExtra("documentId") ?: error("documentId was null!")
                    val revision = intent.getStringExtra("revision") ?: error("revision was null!")
                    val sourceId = intent.getStringExtra("sourceId") ?: error("sourceId was null!")
                    if (isScreenUnlocked()) {
                        if (CancelCommandService.serviceHelper.isServiceRunning(service)) {
                            LOGGER.info("CancelCommandService is already running, so we can't start it again")
                            val notificationId = CANCEL_SCHEDULED_COMMAND_ALREADY_RUNNING_NOTIFICATION_ID
                            service.getManager().notify(notificationId, NotificationHandler.createCancelCommandAlreadyRunningNotification(service, notificationId))
                        } else {
                            val serviceIntent = Intent(context, CancelCommandService::class.java).apply {
                                putExtra("documentId", documentId)
                                putExtra("revision", revision)
                                putExtra("sourceId", sourceId)
                            }
                            service.startForegroundService(serviceIntent)
                        }
                        LOGGER.info("Cancelling scheduled command: $documentId $revision unlocked: ${isScreenUnlocked()}")
                    } else {
                        LOGGER.info("Cannot cancel command when the screen is locked!")
                        Toast.makeText(service, "Must unlock device", Toast.LENGTH_LONG).show()
//                        getKeyguardManager().requestDismissKeyguard(..., null)
                        // We cannot force an unlock of the screen without an activity unless we're on Android 12
                    }
                }
            }
        }
    }
}