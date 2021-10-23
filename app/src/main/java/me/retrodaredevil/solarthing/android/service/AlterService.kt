package me.retrodaredevil.solarthing.android.service

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import org.slf4j.LoggerFactory

class AlterService(
        private val service: Service
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AlterService::class.java)
    }

    fun register() {
        val handler = Handler(Looper.getMainLooper()) // This will cause the receiver to receive the event on a thread other than the main thread
        service.registerReceiver(receiver, IntentFilter().apply { addAction(AlterUpdater.CANCEL_COMMAND_ACTION) }, null, handler)
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
                    if (isScreenUnlocked()) {
                        LOGGER.info("Cancelling scheduled command: $documentId $revision unlocked: ${isScreenUnlocked()}")
                    } else {
                        LOGGER.info("Cannot cancel command when the screen is locked!")
                        // TODO maybe tell the user they need to unlock the screen?
//                        getKeyguardManager().requestDismissKeyguard(..., null)
                        // We cannot force an unlock of the screen without an activity unless we're on Android 12
                    }
                }
            }
        }
    }
}