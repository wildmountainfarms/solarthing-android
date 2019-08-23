package me.retrodaredevil.solarthing.android

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import me.retrodaredevil.solarthing.android.notifications.NotificationChannelGroups
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.android.service.restartService
import me.retrodaredevil.solarthing.android.service.startServiceIfNotRunning
import me.retrodaredevil.solarthing.android.service.stopService
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var connectionProfileManager: ProfileManager<ConnectionProfile>
    private lateinit var connectionProfileHeader: ProfileHeaderHandler
    private lateinit var solarProfileManager: ProfileManager<SolarProfile>
    private lateinit var solarProfileHeader: ProfileHeaderHandler
    private lateinit var miscProfileProvider: ProfileProvider<MiscProfile>

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

    private lateinit var maxFragmentTime: EditText
    private lateinit var startOnBoot: CheckBox

    // region Initialization
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        virtualFloatModeMinimumBatteryVoltage = findViewById(R.id.virtual_float_mode_minimum_battery_voltage)
        startOnBoot = findViewById(R.id.start_on_boot)

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
                        description = channelGroup.getDescription(this@MainActivity)
                    }
                })
            }
            for(notificationChannel in NotificationChannels.values()){
                notificationManager.createNotificationChannel(NotificationChannel(
                    notificationChannel.id,
                    notificationChannel.getName(this),
                    notificationChannel.importance
                ).apply {
                    description = notificationChannel.getDescription(this@MainActivity)
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
    // endregion

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
    @Suppress("UNUSED_PARAMETER")
    fun openCommands(view: View){
        startActivity(Intent(this, CommandActivity::class.java))
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
        }

        if(reloadSettings) loadSettings()
        if(showToast) Toast.makeText(this, "Saved settings!", Toast.LENGTH_SHORT).show()
    }
    private fun saveConnectionSettings(){
        connectionProfileManager.setProfileName(connectionProfileManager.activeUUID, connectionProfileHeader.profileName)
        connectionProfileManager.activeProfile.apply {
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
        solarProfileManager.setProfileName(solarProfileManager.activeUUID, solarProfileHeader.profileName)
        solarProfileManager.activeProfile.let {
            it.generatorFloatTimeHours = generatorFloatHours.text.toString().toFloatOrNull() ?: DefaultOptions.generatorFloatTimeHours
            it.virtualFloatMinimumBatteryVoltage = virtualFloatModeMinimumBatteryVoltage.text.toString().toFloatOrNull() ?: DefaultOptions.virtualFloatModeMinimumBatteryVoltage
            it.lowBatteryVoltage = lowBatteryVoltage.text.toString().toFloatOrNull() ?: DefaultOptions.lowBatteryVoltage
            it.criticalBatteryVoltage = criticalBatteryVoltage.text.toString().toFloatOrNull() ?: DefaultOptions.criticalBatteryVoltage
        }
    }
    private fun loadSettings(){
        connectionProfileManager.activeUUID.let{
            loadConnectionSettings(it)
            connectionProfileHeader.loadSpinner(it)
        }
        solarProfileManager.activeUUID.let {
            loadSolarSettings(it)
            solarProfileHeader.loadSpinner(it)
        }

        miscProfileProvider.activeProfile.let {
            maxFragmentTime.setText(it.maxFragmentTimeMinutes.toString())
            startOnBoot.isChecked = it.startOnBoot
        }
    }
    private fun loadConnectionSettings(uuid: UUID) {
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
            generatorFloatHours.setText(it.generatorFloatTimeHours.toString())
            virtualFloatModeMinimumBatteryVoltage.setText(it.virtualFloatMinimumBatteryVoltage?.toString() ?: "")
            lowBatteryVoltage.setText(it.lowBatteryVoltage?.toString() ?: "")
            criticalBatteryVoltage.setText(it.criticalBatteryVoltage?.toString() ?: "")
        }
    }
    // endregion
}
