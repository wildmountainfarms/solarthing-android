package me.retrodaredevil.solarthing.android.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import me.retrodaredevil.couchdb.CouchProperties
import me.retrodaredevil.couchdb.CouchPropertiesBuilder
import me.retrodaredevil.couchdb.DocumentWrapper
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarThingApplication
import me.retrodaredevil.solarthing.android.createConnectionProfileManager
import me.retrodaredevil.solarthing.android.prefs.ConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.CouchDbDatabaseConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import me.retrodaredevil.solarthing.android.util.createHttpClient
import me.retrodaredevil.solarthing.android.util.initializeDrawer
import me.retrodaredevil.solarthing.commands.CommandInfo
import me.retrodaredevil.solarthing.commands.packets.open.ImmutableRequestCommandPacket
import me.retrodaredevil.solarthing.commands.packets.status.AvailableCommandsPacket
import me.retrodaredevil.solarthing.packets.collection.*
import me.retrodaredevil.solarthing.packets.instance.InstanceSourcePacket
import me.retrodaredevil.solarthing.packets.instance.InstanceSourcePackets
import me.retrodaredevil.solarthing.packets.instance.InstanceTargetPackets
import me.retrodaredevil.solarthing.packets.security.ImmutableAuthNewSenderPacket
import me.retrodaredevil.solarthing.packets.security.ImmutableIntegrityPacket
import me.retrodaredevil.solarthing.packets.security.crypto.Encrypt
import me.retrodaredevil.solarthing.packets.security.crypto.KeyUtil
import me.retrodaredevil.solarthing.util.JacksonUtil
import org.ektorp.impl.StdCouchDbConnector
import org.ektorp.impl.StdCouchDbInstance
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import javax.crypto.Cipher

private fun getAvailableCommands(application: SolarThingApplication): Pair<String, Map<Int, List<CommandInfo>>>? {
    val packetGroups = application.solarStatusData?.packetGroups ?: return null
    val sortedMap = PacketGroups.sortPackets(packetGroups, DefaultInstanceOptions.DEFAULT_DEFAULT_INSTANCE_OPTIONS, 2 * 60 * 1000)
    if (sortedMap.isEmpty()) {
        return null
    }
    val sourceId = sortedMap.keys.first() // TODO sourceId
    val r = HashMap<Int, List<CommandInfo>>()
    for (packetGroup in sortedMap[sourceId]!!) {
        if (packetGroup.dateMillis + 5 * 60 * 1000 < System.currentTimeMillis()) {
            continue // old packet
        }
        for (packet in packetGroup.packets) {
            if (packet is AvailableCommandsPacket) {
                r[packetGroup.getFragmentId(packet)] = packet.commandInfoList // since this is ordered oldest to newest, the newest value should be put in here
            }
        }
    }
    return Pair(sourceId, r)
}

class CommandActivity : AppCompatActivity() {
    companion object {
        private val MAPPER = JacksonUtil.defaultMapper()
    }

    private val cipher = Cipher.getInstance(KeyUtil.CIPHER_TRANSFORMATION)

    private lateinit var profileManager: ProfileManager<ConnectionProfile>

    private lateinit var sender: String
    private lateinit var publicKeyText: TextView
    private lateinit var fragmentSpinner: Spinner
    private lateinit var commandSpinner: Spinner
    private lateinit var currentTaskText: TextView

    private var keyPair: KeyPair? = null

    private var currentTask: AsyncTask<*, *, *>? = null

    private var currentCommands: List<CommandInfo> = emptyList()
    private var sourceId: String? = null
    private var fragmentId: Int? = null

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)
        initializeDrawer(this)
        profileManager = createConnectionProfileManager(this)
        sender = "android-${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}"

        findViewById<TextView>(R.id.sender_name).text = sender
        fragmentSpinner = findViewById(R.id.command_fragment_spinner)
        commandSpinner = findViewById(R.id.command_spinner)
        publicKeyText = findViewById(R.id.public_key)
        currentTaskText = findViewById(R.id.current_task)
        updateKeyPair()
        setToNoTask()
        initFragmentSpinner()
    }
    private fun initFragmentSpinner() {
        val (sourceId, availableCommandsMap) = getAvailableCommands(application as SolarThingApplication) ?: Pair(InstanceSourcePacket.UNUSED_SOURCE_ID, emptyMap())
        this.sourceId = sourceId
        val keys = ArrayList(TreeSet(PacketGroups.DEFAULT_FRAGMENT_ID_COMPARATOR).apply {
            addAll(availableCommandsMap.keys)
        })
        fragmentSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, keys.map { "$it" })
        val currentPosition = fragmentSpinner.selectedItemPosition
        if (currentPosition == AdapterView.INVALID_POSITION) {
            updateSpinner(emptyList())
            fragmentId = null
        } else {
            val fragmentId = keys[currentPosition]
            this.fragmentId = fragmentId
            updateSpinner(availableCommandsMap[fragmentId] ?: error("No command list for fragmentId=$fragmentId"))
        }
        fragmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateSpinner(emptyList())
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val fragmentId = keys[position]
                updateSpinner(availableCommandsMap[fragmentId] ?: error("No command list for fragmentId=$fragmentId"))
            }
        }
    }
    private fun updateSpinner(commands: List<CommandInfo>) {
        currentCommands = commands
        commandSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, commands.map { it.displayName })
    }
    private fun getSelectedCommand(): CommandInfo? {
        val itemId = commandSpinner.selectedItemPosition
        if (itemId == AdapterView.INVALID_POSITION) {
            return null
        }
        return currentCommands[itemId]
    }

    @Suppress("UNUSED_PARAMETER")
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
    @Suppress("UNUSED_PARAMETER")
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
    @Suppress("UNUSED_PARAMETER")
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
    private fun getCouchProperties(): CouchProperties {
        return (profileManager.activeProfile.profile.databaseConnectionProfile as CouchDbDatabaseConnectionProfile).createCouchProperties()
    }
    @SuppressLint("SetTextI18n")
    private fun sendAuthRequest(publicKey: PublicKey){
        if(checkCurrentTask()) return

        val packet = ImmutableAuthNewSenderPacket(sender, KeyUtil.encodePublicKey(publicKey))

        currentTaskText.text = "Sending Auth Request"
        currentTask = CouchDbUploadToDatabase(
                getCouchProperties(),
                PacketCollections.createFromPackets(listOf(packet), PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR, TimeZone.getDefault()),
                ::onPostExecute
        ).execute()
    }
    @SuppressLint("SetTextI18n")
    @Suppress("UNUSED_PARAMETER")
    fun sendCommand(view: View){
        val keyPair = this.keyPair
        if(keyPair == null){
            Toast.makeText(this, "Please generate a key!", Toast.LENGTH_SHORT).show()
            return
        }
        if(checkCurrentTask()) return

        val selectedCommand = getSelectedCommand()
        val instancePackets = arrayOf(
                InstanceSourcePackets.create(sourceId!!),
                InstanceTargetPackets.create(listOf(fragmentId!!))
        )
        if (selectedCommand == null) {
            Toast.makeText(this, "No selected command", Toast.LENGTH_SHORT).show()
            return
        }

        println("Going to send command: ${selectedCommand.name}")
        val encryptedCollection = PacketCollections.createFromPackets(listOf(
                ImmutableRequestCommandPacket(selectedCommand.name),
                *instancePackets
        ), PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR, TimeZone.getDefault())

        val text = System.currentTimeMillis().toString(16) + "," + MAPPER.writeValueAsString(encryptedCollection)
        println(text)
        val encrypted = Encrypt.encrypt(cipher, keyPair.private, text)

        currentTaskText.text = "Sending command"
        currentTask = CouchDbUploadToDatabase(
                getCouchProperties(),
                PacketCollections.createFromPackets(
                        listOf(
                                ImmutableIntegrityPacket(sender, encrypted),
                                *instancePackets
                        ),
                        PacketCollectionIdGenerator.Defaults.UNIQUE_GENERATOR, TimeZone.getDefault()
                ),
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
    @SuppressLint("SetTextI18n")
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
    @Suppress("UNUSED_PARAMETER")
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
                KeyUtil.decodePrivateKey(it.readBytes())
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
private class CouchDbUploadToDatabase(
    private val couchProperties: CouchProperties,
    private val packetCollection: PacketCollection,
    private val onFinish: (Boolean?) -> Unit
) : AsyncTask<Void, Void, Boolean>() {
    override fun doInBackground(vararg params: Void?): Boolean {
        try {
            val httpClient = createHttpClient(CouchPropertiesBuilder(couchProperties)
                    .setConnectionTimeoutMillis(10_000)
                    .setSocketTimeoutMillis(Int.MAX_VALUE)
                    .build())
            val instance = StdCouchDbInstance(httpClient)
            val client = StdCouchDbConnector("commands", instance)
            client.createDatabaseIfNotExists()
            client.create(DocumentWrapper(packetCollection.dbId).apply {
                `object` = packetCollection
            })
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
