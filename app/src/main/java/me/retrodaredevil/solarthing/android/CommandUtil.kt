package me.retrodaredevil.solarthing.android

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import me.retrodaredevil.solarthing.packets.security.crypto.KeyUtil
import java.io.FileNotFoundException
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

fun getSavedKeyPair(context: Context): KeyPair?{
    return try {
        val publicKey: PublicKey = context.openFileInput(".publickey").use {
            KeyUtil.decodePublicKey(it.readBytes())
        }
        val privateKey: PrivateKey = context.openFileInput(".privatekey").use {
            KeyUtil.decodePrivateKey(it.readBytes())
        }
        KeyPair(publicKey, privateKey)
    } catch(ex: FileNotFoundException){
        null
    }
}

@SuppressLint("HardwareIds")
fun getSenderName(context: Context): String {
    return "android-${Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)}"
}
