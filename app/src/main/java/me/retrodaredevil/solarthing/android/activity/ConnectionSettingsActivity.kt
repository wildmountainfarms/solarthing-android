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
import me.retrodaredevil.solarthing.android.createMiscProfileProvider
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
                this::saveConnectionSettings, // TODO problem here!!!
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
                saveSettings = { saveSettings() }
        )

        useAuth.setOnCheckedChangeListener{ _, _ ->
            onUseAuthUpdate()
        }

        preferredSourceIdSpinner.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val application = application as SolarThingApplication
                val sourceIdSet = mutableSetOf<String>()
                // TODO maybe don't iterate over *all* the packets
                application.solarStatusData?.useCache { it.createAllCachedPacketsStream(false).forEach { storedPacketGroup ->
                    val instancePacketGroup = PacketGroups.parseToInstancePacketGroup(storedPacketGroup, DefaultInstanceOptions.DEFAULT_DEFAULT_INSTANCE_OPTIONS)
                    sourceIdSet.add(instancePacketGroup.sourceId)
                } } // This puts a lot on other things that want to use useCache, but that's OK, this shouldn't take long, and it doesn't happen often
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

        val miscProfile = createMiscProfileProvider(this).activeProfile.profile
        connectionProfileNetworkSwitchingViewHandler.show(miscProfile.networkSwitchingEnabled)
    }

    private fun requestLocationPrePopup() {
        AlertDialog.Builder(this)
                .setTitle("Grant Location Permission?")
                .setMessage("SolarThing needs the location permission to view your WiFi's SSID (network name). This is required if you want to enable auto network switching. Please allow all the time.")
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("Yes") { _, _ ->
                    startFineToBackgroundLocationRequestChain()
                }
                .create().show()
    }

    private fun startFineToBackgroundLocationRequestChain(){
        // As of target SDK 33, you CANNOT ask for BACKGROUND without first asking for COARSE or FINE
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION_RC)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_FINE_LOCATION_RC){
            val success = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if(success){
                if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION) {
                    Toast.makeText(this, "You will now be prompted for background location access.", Toast.LENGTH_SHORT).show()
                    val permission = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        else -> Manifest.permission.ACCESS_FINE_LOCATION
                    }
                    ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_FINE_LOCATION_RC)
                } else {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                }
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
            username.visibility = View.VISIBLE
            password.visibility = View.VISIBLE
        } else {
            username.visibility = View.GONE
            password.visibility = View.GONE
        }
    }
    // region Saving and Loading
    private fun saveSettings(showToast: Boolean = true){
        val uuid = connectionProfileHeader.editUUID
        saveConnectionSettings(uuid)
        connectionProfileManager.setProfileName(uuid, connectionProfileHeader.profileName)

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
        connectionProfileManager.getProfile(uuid).profile = getConnectionProfile()
    }
    private fun loadSettings(){
        connectionProfileHeader.loadSpinner(connectionProfileHeader.editUUID)
    }
    private fun loadConnectionSettings(uuid: UUID) {
        val profile = connectionProfileManager.getProfile(uuid).profile
        connectionProfileNetworkSwitchingViewHandler.load(profile.networkSwitchingProfile)

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
