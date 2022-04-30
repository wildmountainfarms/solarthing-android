package me.retrodaredevil.solarthing.android

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.wearable.complications.ComplicationProviderService.EXTRA_COMPLICATION_ID
import android.support.wearable.complications.ProviderUpdateRequester
import android.support.wearable.complications.ProviderUpdateRequester.EXTRA_PROVIDER_COMPONENT

/**
 * Returns a pending intent, suitable for use as a tap intent, that causes a complication to be
 * toggled and updated.
 */
fun getToggleIntent(context: Context, provider: ComponentName, complicationId: Int): PendingIntent {
    val intent = Intent(context, TapBroadcastReceiver::class.java)
    intent.putExtra(EXTRA_PROVIDER_COMPONENT, provider)
    intent.putExtra(EXTRA_COMPLICATION_ID, complicationId)

    // Pass complicationId as the requestCode to ensure that different complications get
    // different intents.
    return PendingIntent.getBroadcast(context, complicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class TapBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras!!
        val provider: ComponentName = extras.getParcelable(EXTRA_PROVIDER_COMPONENT) ?: error("Doesn't have provider component! extras: $extras intent: $intent")
        val complicationId = extras.getInt(EXTRA_COMPLICATION_ID)
        val requester = ProviderUpdateRequester(context, provider)
        requester.requestUpdate(complicationId)
        println("Requested update (from tap)")
    }
}
