package me.retrodaredevil.solarthing.android.activity

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import me.retrodaredevil.solarthing.android.SolarThingApplication
import me.retrodaredevil.solarthing.android.StartupHelper
import me.retrodaredevil.solarthing.android.createConnectionProfileManager
import me.retrodaredevil.solarthing.android.prefs.ConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.CouchDbDatabaseConnectionProfile
import me.retrodaredevil.solarthing.android.prefs.DefaultOptions
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import me.retrodaredevil.solarthing.android.util.DrawerHandler
import me.retrodaredevil.solarthing.android.util.initializeDrawerWithUnsavedPrompt
import me.retrodaredevil.solarthing.packets.collection.DefaultInstanceOptions
import me.retrodaredevil.solarthing.packets.collection.PacketGroups
import java.util.*

class ConnectionSettingsActivity  : AppCompatActivity() {
    companion object {
        private const val REQUEST_FINE_LOCATION_RC = 1801
    }

    private lateinit var connectionProfileManager: ProfileManager<ConnectionProfile>
    private lateinit var connectionProfileHeader: ProfileHeaderHandler

    private lateinit var connectionProfileNetworkSwitchingViewHandler: NetworkSwitchingViewHandler
    private lateinit var protocol: EditText
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var useAuth: CheckBox

    private lateinit var initialRequestTimeout: EditText
    private lateinit var subsequentRequestTimeout: EditText

    private lateinit var preferredSourceIdEditText: EditText
    private lateinit var preferredSourceIdSpinner: Spinner


    private lateinit var drawerHandler: DrawerHandler

    // region Initialization
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(me.retrodaredevil.solarthing.android.R.layout.activity_settings_connection)


        connectionProfileManager = createConnectionProfileManager(this)
        connectionProfileHeader = ProfileHeaderHandler(
                this,
                findViewById(me.retrodaredevil.solarthing.android.R.id.connection_profile_header_layout),
                connectionProfileManager,
                this::saveConnectionSettings,
                this::loadConnectionSettings
        )

        connectionProfileNetworkSwitchingViewHandler = NetworkSwitchingViewHandler(this, findViewById(
                me.retrodaredevil.solarthing.android.R.id.network_switching)) {
            requestLocationPrePopup()
        }
        protocol = findViewById(me.retrodaredevil.solarthing.android.R.id.protocol)
        host = findViewById(me.retrodaredevil.solarthing.android.R.id.hostname)
        port = findViewById(me.retrodaredevil.solarthing.android.R.id.port)
        username = findViewById(me.retrodaredevil.solarthing.android.R.id.username)
        password = findViewById(me.retrodaredevil.solarthing.android.R.id.password)
        useAuth = findViewById(me.retrodaredevil.solarthing.android.R.id.use_auth)
        initialRequestTimeout = findViewById(me.retrodaredevil.solarthing.android.R.id.initial_request_timeout)
        subsequentRequestTimeout = findViewById(me.retrodaredevil.solarthing.android.R.id.subsequent_request_timeout)
        preferredSourceIdEditText = findViewById(me.retrodaredevil.solarthing.android.R.id.preferred_source_id)
        preferredSourceIdSpinner = findViewById(me.retrodaredevil.solarthing.android.R.id.preferred_source_id_spinner)

        drawerHandler = initializeDrawerWithUnsavedPrompt(
                this,
                isNotSavedGetter = { isConnectionProfileNotSaved() },
                saveSettings = { saveSettings(false, true) }
        )

        useAuth.setOnCheckedChangeListener{ _, _ ->
            onUseAuthUpdate()
        }

        preferredSourceIdSpinner.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val application = application as SolarThingApplication
                val sourceIdSet = mutableSetOf<String>()
                application.solarStatusData?.packetGroups?.forEach {
                    val instancePacketGroup = PacketGroups.parseToInstancePacketGroup(it, DefaultInstanceOptions.DEFAULT_DEFAULT_INSTANCE_OPTIONS)
                    sourceIdSet.add(instancePacketGroup.sourceId)
                }
                val sourceIdList = LinkedList(sourceIdSet)
                val currentSourceId = preferredSourceIdEditText.text.toString()
                sourceIdList.remove(currentSourceId)
                sourceIdList.addFirst(currentSourceId)

                preferredSourceIdSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sourceIdList)
                preferredSourceIdSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>) {}

                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                        preferredSourceIdEditText.setText(sourceIdList[position])
                    }
                }
            }
            v.performClick() // This is here for some reason because Android Studio likes it
            false
        }

        loadSettings()

        StartupHelper(this).onStartup()
    }

    override fun onResume() {
        super.onResume()
        drawerHandler.closeDrawer()
        drawerHandler.highlight()
    }

    private fun requestLocationPrePopup() {
        AlertDialog.Builder(this)
                .setTitle("Grant Location Permission?")
                .setMessage("SolarThing needs the location permission to view your WiFi's SSID (network name). This is required if you want to enable auto network switching. Please allow all the time.")
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("Yes") { _, _ ->
                    requestFineLocation()
                }
                .create().show()
    }

    private fun requestFineLocation(){
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION) else arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_FINE_LOCATION_RC)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == REQUEST_FINE_LOCATION_RC){
            val success = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if(success){
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
    @Suppress("UNUSED_PARAMETER")
    fun saveSettings(view: View){
        saveSettings()
    }

    private fun onUseAuthUpdate(){
        if(useAuth.isChecked){
            username.alpha = 1f
            password.alpha = 1f
        } else {
            username.alpha = .5f
            password.alpha = .5f
        }
    }
    // region Saving and Loading
    private fun saveSettings(reloadSettings: Boolean = true, showToast: Boolean = true){
        saveConnectionSettings(connectionProfileHeader.editUUID)

        if(reloadSettings) loadSettings()
        if(showToast) Toast.makeText(this, "Saved settings!", Toast.LENGTH_SHORT).show()
    }
    private fun getConnectionProfile() = ConnectionProfile(
            CouchDbDatabaseConnectionProfile(
                    protocol.text.toString(),
                    host.text.toString(),
                    port.text.toString().toIntOrNull() ?: DefaultOptions.CouchDb.port,
                    username.text.toString(),
                    password.text.toString(),
                    useAuth.isChecked
            ),
            connectionProfileNetworkSwitchingViewHandler.getNetworkSwitchingProfile(),
            initialRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.initialRequestTimeSeconds,
            subsequentRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.subsequentRequestTimeSeconds,
            preferredSourceIdEditText.text.toString().let { if (it.isEmpty()) null else it } // a blank source ID represents no preference
    )
    private fun isConnectionProfileNotSaved() = getConnectionProfile() != connectionProfileManager.getProfile(connectionProfileHeader.editUUID).profile
    private fun saveConnectionSettings(uuid: UUID){
        connectionProfileManager.setProfileName(uuid, connectionProfileHeader.profileName)
        connectionProfileManager.getProfile(uuid).profile = getConnectionProfile()
    }
    private fun loadSettings(){
        connectionProfileHeader.editUUID.let{
            loadConnectionSettings(it)
            connectionProfileHeader.loadSpinner(it)
        }
    }
    private fun loadConnectionSettings(uuid: UUID) {
        val profile = connectionProfileManager.getProfile(uuid).profile
        connectionProfileNetworkSwitchingViewHandler.load(profile.networkSwitchingProfile)

        val name = connectionProfileManager.getProfileName(uuid)
        connectionProfileHeader.profileName = name
        (profile.databaseConnectionProfile as CouchDbDatabaseConnectionProfile).let {
            protocol.setText(it.protocol)
            host.setText(it.host)
            port.setText(it.port.toString())
            username.setText(it.username)
            password.setText(it.password)
            useAuth.isChecked = it.useAuth
            onUseAuthUpdate()
        }
        profile.let {
            initialRequestTimeout.setText(it.initialRequestTimeSeconds.toString())
            subsequentRequestTimeout.setText(it.subsequentRequestTimeSeconds.toString())
            preferredSourceIdEditText.setText(it.preferredSourceId ?: "")
        }
    }
    // endregion
}