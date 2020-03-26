package me.retrodaredevil.solarthing.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.*
import me.retrodaredevil.solarthing.android.notifications.NotificationChannelGroups
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.android.service.restartService
import me.retrodaredevil.solarthing.android.service.startServiceIfNotRunning
import me.retrodaredevil.solarthing.android.service.stopService
import java.util.*

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_COARSE_LOCATION_RC = 1801
    }

    private lateinit var connectionProfileManager: ProfileManager<ConnectionProfile>
    private lateinit var connectionProfileHeader: ProfileHeaderHandler
    private lateinit var solarProfileManager: ProfileManager<SolarProfile>
    private lateinit var solarProfileHeader: ProfileHeaderHandler
    private lateinit var miscProfileProvider: ProfileProvider<MiscProfile>

    private lateinit var connectionProfileNetworkSwitchingViewHandler: NetworkSwitchingViewHandler
    private lateinit var protocol: EditText
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var useAuth: CheckBox

    private lateinit var initialRequestTimeout: EditText
    private lateinit var subsequentRequestTimeout: EditText


    private lateinit var generatorFloatHours: EditText
    private lateinit var virtualFloatModeMinimumBatteryVoltage: EditText
    private lateinit var lowBatteryVoltage: EditText
    private lateinit var criticalBatteryVoltage: EditText
    private lateinit var batteryVoltageTypeSpinner: Spinner

    private lateinit var maxFragmentTime: EditText
    private lateinit var startOnBoot: CheckBox
    private lateinit var networkSwitchingEnabledCheckBox: CheckBox

    // region Initialization
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


        connectionProfileManager = createConnectionProfileManager(this)
        connectionProfileHeader = ProfileHeaderHandler(
            this,
            findViewById(R.id.connection_profile_header_layout),
            connectionProfileManager,
            this::saveConnectionSettings,
            this::loadConnectionSettings
        )

        solarProfileManager = createSolarProfileManager(this)
        solarProfileHeader = ProfileHeaderHandler(
            this,
            findViewById(R.id.solar_profile_header_layout),
            solarProfileManager,
            this::saveSolarSettings,
            this::loadSolarSettings
        )

        miscProfileProvider = createMiscProfileProvider(this)

        connectionProfileNetworkSwitchingViewHandler = NetworkSwitchingViewHandler(
            this, findViewById(R.id.network_switching), { connectionProfileManager.getProfile(connectionProfileHeader.editUUID).networkSwitchingProfile },
            {
                requestCoarseLocation()
            }
        )
        protocol = findViewById(R.id.protocol)
        host = findViewById(R.id.hostname)
        port = findViewById(R.id.port)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        useAuth = findViewById(R.id.use_auth)
        generatorFloatHours = findViewById(R.id.generator_float_hours)
        initialRequestTimeout = findViewById(R.id.initial_request_timeout)
        subsequentRequestTimeout = findViewById(R.id.subsequent_request_timeout)
        maxFragmentTime = findViewById(R.id.max_fragment_time)
        lowBatteryVoltage = findViewById(R.id.low_battery_voltage)
        criticalBatteryVoltage = findViewById(R.id.critical_battery_voltage)
        batteryVoltageTypeSpinner = findViewById(R.id.battery_type_spinner)
        virtualFloatModeMinimumBatteryVoltage = findViewById(R.id.virtual_float_mode_minimum_battery_voltage)
        startOnBoot = findViewById(R.id.start_on_boot)
        networkSwitchingEnabledCheckBox = findViewById(R.id.network_switching_enabled)

        initializeDrawer(this)

        useAuth.setOnCheckedChangeListener{ _, _ ->
            onUseAuthUpdate()
        }

        loadSettings()


        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for(channel in NotificationChannels.OLD_CHANNELS) {
                notificationManager.deleteNotificationChannel(channel)
            }

            for(channelGroup in NotificationChannelGroups.values()){
                notificationManager.createNotificationChannelGroup(NotificationChannelGroup(
                    channelGroup.id, channelGroup.getName(this)
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        description = channelGroup.getDescription(this@SettingsActivity)
                    }
                })
            }
            for(notificationChannel in NotificationChannels.values()){
                notificationManager.createNotificationChannel(NotificationChannel(
                    notificationChannel.id,
                    notificationChannel.getName(this),
                    notificationChannel.importance
                ).apply {
                    description = notificationChannel.getDescription(this@SettingsActivity)
                    enableLights(notificationChannel.enableLights)
                    if (notificationChannel.lightColor != null) {
                        lightColor = notificationChannel.lightColor
                    }
                    enableVibration(notificationChannel.enableVibration)
                    if (notificationChannel.vibrationPattern != null) {
                        vibrationPattern = notificationChannel.vibrationPattern
                    }
                    setShowBadge(notificationChannel.showBadge)
                    if(notificationChannel.notificationChannelGroups != null){
                        group = notificationChannel.notificationChannelGroups.id
                    }
                    if(notificationChannel.sound != null){
                        val soundId = notificationChannel.sound
                        val uri = Uri.parse("android.resource://$packageName/raw/$soundId")
                        setSound(
                            uri,
                            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build()
                        )
                    }
                })
            }
        }
        startServiceIfNotRunning(this)
    }

    private fun requestCoarseLocation(){
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_COARSE_LOCATION_RC)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == REQUEST_COARSE_LOCATION_RC){
            val success = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if(success){
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // region Buttons
    @Suppress("UNUSED_PARAMETER")
    fun saveSettings(view: View){
        saveSettings()
    }
    @Suppress("UNUSED_PARAMETER")
    fun restartService(view: View){
        restartService(this)
    }
    @Suppress("UNUSED_PARAMETER")
    fun stopService(view: View){
        stopService(this)
    }
    // endregion

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
        saveConnectionSettings()
        saveSolarSettings()
        miscProfileProvider.activeProfile.let {
            it.maxFragmentTimeMinutes = maxFragmentTime.text.toString().toFloatOrNull() ?: DefaultOptions.maxFragmentTimeMinutes
            it.startOnBoot = startOnBoot.isChecked
            it.networkSwitchingEnabled = networkSwitchingEnabledCheckBox.isChecked
        }

        if(reloadSettings) loadSettings()
        if(showToast) Toast.makeText(this, "Saved settings!", Toast.LENGTH_SHORT).show()
    }
    private fun saveConnectionSettings(){
        val uuid = connectionProfileHeader.editUUID
        connectionProfileManager.setProfileName(uuid, connectionProfileHeader.profileName)
        connectionProfileNetworkSwitchingViewHandler.save() // as of right now only connection profiles have a network switching handler
        connectionProfileManager.getProfile(uuid).apply {
            (databaseConnectionProfile as CouchDbDatabaseConnectionProfile).let { // TODO don't cast
                it.protocol = protocol.text.toString()
                it.host = host.text.toString()
                it.port = port.text.toString().toIntOrNull() ?: DefaultOptions.CouchDb.port
                it.username = username.text.toString()
                it.password = password.text.toString()
                it.useAuth = useAuth.isChecked
            }
            initialRequestTimeSeconds = initialRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.initialRequestTimeSeconds
            subsequentRequestTimeSeconds = subsequentRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.subsequentRequestTimeSeconds
        }
    }
    private fun saveSolarSettings(){
        val uuid = solarProfileHeader.editUUID
        solarProfileManager.setProfileName(uuid, solarProfileHeader.profileName)
        solarProfileManager.getProfile(uuid).let {
            it.voltageTimerTimeHours = generatorFloatHours.text.toString().toFloatOrNull() ?: DefaultOptions.generatorFloatTimeHours
            it.voltageTimerBatteryVoltage = virtualFloatModeMinimumBatteryVoltage.text.toString().toFloatOrNull()
            it.lowBatteryVoltage = lowBatteryVoltage.text.toString().toFloatOrNull()
            it.criticalBatteryVoltage = criticalBatteryVoltage.text.toString().toFloatOrNull()
            val position = batteryVoltageTypeSpinner.selectedItemPosition
            if(position != AdapterView.INVALID_POSITION){
                it.batteryVoltageType = BatteryVoltageType.values()[position]
            }
        }
    }
    private fun loadSettings(){
        connectionProfileHeader.editUUID.let{
            loadConnectionSettings(it)
            connectionProfileHeader.loadSpinner(it)
        }
        solarProfileHeader.editUUID.let {
            loadSolarSettings(it)
            solarProfileHeader.loadSpinner(it)
        }

        miscProfileProvider.activeProfile.let {
            maxFragmentTime.setText(it.maxFragmentTimeMinutes.toString())
            startOnBoot.isChecked = it.startOnBoot
            networkSwitchingEnabledCheckBox.isChecked = it.networkSwitchingEnabled
        }
    }
    private fun loadConnectionSettings(uuid: UUID) {
        connectionProfileNetworkSwitchingViewHandler.load()
        val profile = connectionProfileManager.getProfile(uuid)
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
        }
    }
    private fun loadSolarSettings(uuid: UUID) {
        val profile = solarProfileManager.getProfile(uuid)
        val name = solarProfileManager.getProfileName(uuid)
        solarProfileHeader.profileName = name
        profile.let {
            generatorFloatHours.setText(it.voltageTimerTimeHours.toString())
            virtualFloatModeMinimumBatteryVoltage.setText(it.voltageTimerBatteryVoltage?.toString() ?: "")
            lowBatteryVoltage.setText(it.lowBatteryVoltage?.toString() ?: "")
            criticalBatteryVoltage.setText(it.criticalBatteryVoltage?.toString() ?: "")

            batteryVoltageTypeSpinner.let { spinner ->
                val array = BatteryVoltageType.values()
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, getNiceBatteryVoltageStringList(array))
                val batteryVoltageType = it.batteryVoltageType
                var selection: Int? = null
                for((i, type) in array.withIndex()){
                    if(type == batteryVoltageType){
                        selection = i
                    }
                }
                selection!!
                spinner.setSelection(selection)
            }
        }
    }
    // endregion
    private fun getNiceBatteryVoltageStringList(values: Array<BatteryVoltageType>): List<String> {
        return values.map {
            when(it){
                BatteryVoltageType.AVERAGE -> "Average"
                BatteryVoltageType.FIRST_PACKET -> "First packet"
                BatteryVoltageType.MOST_RECENT -> "Most recent"
                BatteryVoltageType.FIRST_OUTBACK -> "First Outback"
                BatteryVoltageType.FIRST_OUTBACK_FX -> "First Outback FX"
            }
        }
    }
}
