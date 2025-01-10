package me.retrodaredevil.solarthing.android.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build


/**
 * A compatibility function for to call the correct [Context#registerReceiver] depending on the SDK version.
 * Using this is required for SDK version >= 34.
 */
@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun registerReceiverNotExported(context: Context, receiver: BroadcastReceiver, intentFilter: IntentFilter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // TIRAMISU is SDK 33 (Android 13)
        // We have access to this on SDK 33, and using it is required when targeting SDK 34 and above
        context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        // We have UnspecifiedRegisterReceiverFlag suppression on this function because the TIRAMISU version doesn't work on previous SDK versions
        context.registerReceiver(receiver, intentFilter)
    }
}

/**
 * This function exists as a simple utility to make an explicit intent.
 *
 * Android 14 (SDK 34) prevents apps from sending implicit intents to internal app components.
 * That means that you should use explicit intents when you want to send messages to internal components.
 *
 * This utility doesn't do anything other than call [Intent.setPackage] with [context]'s [Context.getPackageName] after having created the intent.
 *
 * This function is not designed to change or for extra functionality to be added. If this changes in the future, you should re-evaluate the usages of this function.
 */
fun createExplicitIntent(context: Context, action: String): Intent {
    val intent = Intent(action)
    intent.`package` = context.packageName
    return intent
}
