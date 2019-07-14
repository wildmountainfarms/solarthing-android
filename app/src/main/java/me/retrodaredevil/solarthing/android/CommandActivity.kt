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
    private lateinit var currentTaskText: TextView

    private var keyPair: KeyPair? = null

    private var currentTask: AsyncTask<*, *, *>? = null

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)
        sender = "android-${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}"

        findViewById<TextView>(R.id.sender_name).text = sender
        commandText = findViewById(R.id.command_text)
        publicKeyText = findViewById(R.id.public_key)
        currentTaskText = findViewById(R.id.current_task)
        updateKeyPair()
        setToNoTask()
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
        if(checkCurrentTask()) return

        val packet = ImmutableAuthNewSenderPacket(sender, KeyUtil.encodePublicKey(publicKey))

        currentTaskText.text = "Sending Auth Request"
        currentTask = UploadToDatabase(
            Prefs(this).createCouchProperties()[0], // TODO figure out a better option than just choosing the first value
            PacketCollections.createFromPackets(listOf(packet), PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR),
            ::onPostExecute
        ).execute()
    }
    fun sendCommand(view: View){
        val keyPair = this.keyPair
        if(keyPair == null){
            Toast.makeText(this, "Please generate a key!", Toast.LENGTH_SHORT).show()
            return
        }
        if(checkCurrentTask()) return

        val text = System.currentTimeMillis().toString(16) + "," + commandText.text.toString()
        println("Going to send text: $text")
        val encrypted = Encrypt.encrypt(cipher, keyPair.private, text)

        val packet = ImmutableIntegrityPacket(sender, encrypted)
        currentTaskText.text = "Sending command"
        currentTask = UploadToDatabase(
            Prefs(this).createCouchProperties()[0], // TODO figure out a better option than just choosing the first value
            PacketCollections.createFromPackets(listOf(packet), PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR),
            ::onPostExecute
        ).execute()
    }
    private fun onPostExecute(result: Boolean?){
        if(false == result || result == null){
            Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Task executed successfully", Toast.LENGTH_SHORT).show()
        }
        currentTask = null
        setToNoTask()
    }
    private fun setToNoTask(){
        currentTaskText.text = "None"
    }

    /**
     * If true, you should return and do nothing else because a message has already been sent to the user
     * @return true if there is a current task, false otherwise
     */
    private fun checkCurrentTask(): Boolean{
        if(currentTask != null){
            Toast.makeText(this, "Please cancel the current task!", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }
    fun cancelCurrentTask(view: View){
        val currentTask = this.currentTask
        if(currentTask != null){
            currentTask.cancel(true)
            Toast.makeText(this, "Cancelled task!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No task to cancel!", Toast.LENGTH_SHORT).show()
        }
        this.currentTask = null
        setToNoTask()
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
    private val packetCollection: PacketCollection,
    private val onFinish: (Boolean?) -> Unit
) : AsyncTask<Void, Void, Boolean>() {
    override fun doInBackground(vararg params: Void?): Boolean {
        try {
            val client = CouchDbClientAndroid(CouchPropertiesBuilder(couchProperties).setDatabase("commands").setCreateIfNotExist(true).build().createProperties())
            client.save(packetCollection)
        } catch(ex: Exception){
            ex.printStackTrace()
            return false
        }
        return true
    }

    override fun onPostExecute(result: Boolean?) {
        onFinish(result)
    }

}
