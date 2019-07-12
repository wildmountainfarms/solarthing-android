package me.retrodaredevil.solarthing.android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdb.CouchPropertiesBuilder
import me.retrodaredevil.solarthing.packets.collection.PacketCollection
import me.retrodaredevil.solarthing.packets.collection.PacketCollectionIdGenerator
import me.retrodaredevil.solarthing.packets.collection.PacketCollections
import me.retrodaredevil.solarthing.packets.security.ImmutableAuthNewSenderPacket
import me.retrodaredevil.solarthing.packets.security.ImmutableIntegrityPacket
import me.retrodaredevil.solarthing.packets.security.crypto.Encrypt
import me.retrodaredevil.solarthing.packets.security.crypto.KeyUtil
import org.lightcouch.CouchDbClientAndroid
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher

class CommandActivity : AppCompatActivity() {

    private val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")

    private lateinit var sender: String
    private lateinit var publicKeyText: TextView
    private lateinit var commandText: EditText

    private var keyPair: KeyPair? = null

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)
        sender = "android-${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}"

        findViewById<TextView>(R.id.sender_name).text = sender
        commandText = findViewById(R.id.command_text)
        publicKeyText = findViewById(R.id.public_key)
        updateKeyPair()
    }
    fun generateNewKey(view: View){
        if(keyPair == null){
            generateNewKey()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Replace old key and create new?")
            .setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("Yes") { _, _ ->
                generateNewKey()
            }
            .create().show()
    }
    private fun generateNewKey(){
        setKeyPair(KeyUtil.generateKeyPair())
        Toast.makeText(this, "Generated a new key pair!", Toast.LENGTH_SHORT).show()
    }
    fun deleteKey(view: View){
        if(keyPair == null){
            Toast.makeText(this, "You haven't generated a key pair key!", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete key?")
            .setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("Yes") { _, _ ->
                setKeyPair(null)
                Toast.makeText(this, "Deleted key pair!", Toast.LENGTH_SHORT).show()
            }
            .create().show()
    }
    fun sendAuthRequest(view: View){
        val keyPair = this.keyPair
        if(keyPair == null){
            AlertDialog.Builder(this)
                .setTitle("Please generate a key first!")
                .setNeutralButton("Ok") { _, _ -> }
                .create().show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Would you like to send a new auth request?")
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("Send") { _, _ ->
                    sendAuthRequest(keyPair.public)
                }
                .create().show()
        }
    }
    private fun sendAuthRequest(publicKey: PublicKey){
        val packet = ImmutableAuthNewSenderPacket(sender, KeyUtil.encodePublicKey(publicKey))

        UploadToDatabase(
            Prefs(this).createCouchProperties()[0], // TODO figure out a better option than just choosing the first value
            PacketCollections.createFromPackets(listOf(packet), PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR)
        ).execute()
    }
    fun sendCommand(view: View){
        val keyPair = this.keyPair
        if(keyPair == null){
            Toast.makeText(this, "Please generate a key!", Toast.LENGTH_SHORT).show()
            return
        }
        val text = System.currentTimeMillis().toString(16) + "," + commandText.text.toString()
        println("Going to send text: $text")
        val encrypted = Encrypt.encrypt(cipher, keyPair.private, text)

        val packet = ImmutableIntegrityPacket(sender, encrypted)
        UploadToDatabase(
            Prefs(this).createCouchProperties()[0], // TODO figure out a better option than just choosing the first value
            PacketCollections.createFromPackets(listOf(packet), PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR)
        ).execute()
    }
    @SuppressLint("SetTextI18n")
    private fun updateKeyPair(){
        val keyPair = try {
            val publicKey: PublicKey = openFileInput(".publickey").use {
                KeyUtil.decodePublicKey(it.readBytes())
            }
            val privateKey: PrivateKey = openFileInput(".privatekey").use {
                // TODO add KeyUtil.decodePrivateKey() in solarthing codebase
                val spec = PKCS8EncodedKeySpec(it.readBytes())
                KeyFactory.getInstance("RSA").generatePrivate(spec)
            }
            KeyPair(publicKey, privateKey)
        } catch(ex: FileNotFoundException){
            null
        }
        this.keyPair = keyPair
        if(keyPair == null){
            publicKeyText.text = "none created"
        } else {
            publicKeyText.text = KeyUtil.encodePublicKey(keyPair.public)
        }
    }

    private fun setKeyPair(pair: KeyPair?){
        if(pair == null){
            val publicKeyFile = File(filesDir, ".publickey")
            val privateKeyFile = File(filesDir, ".privatekey")
            publicKeyFile.delete()
            privateKeyFile.delete()
            updateKeyPair()
            return
        }
        openFileOutput(".publickey", Context.MODE_PRIVATE).use {
            it.write(pair.public.encoded)
        }
        openFileOutput(".privatekey", Context.MODE_PRIVATE).use {
            it.write(pair.private.encoded)
        }
        updateKeyPair()
    }
}
private class UploadToDatabase(
    private val couchProperties: CouchProperties,
    private val packetCollection: PacketCollection
) : AsyncTask<Void, Void, Void?>() {
    override fun doInBackground(vararg params: Void?): Void? {
        val client = CouchDbClientAndroid(CouchPropertiesBuilder(couchProperties).setDatabase("commands").setCreateIfNotExist(true).build().createProperties())
        client.save(packetCollection)
        return null
    }

}
