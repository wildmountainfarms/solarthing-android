package me.retrodaredevil.solarthing.android

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import me.retrodaredevil.solarthing.android.notifications.NotificationChannelGroups
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.service.restartService
import me.retrodaredevil.solarthing.android.service.startServiceIfNotRunning
import me.retrodaredevil.solarthing.android.service.stopService

class MainActivity : AppCompatActivity() {

    private val prefs = Prefs(this)

    private lateinit var protocol: EditText
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var useAuth: CheckBox
    private lateinit var generatorFloatHours: EditText
    private lateinit var initialRequestTimeout: EditText
    private lateinit var subsequentRequestTimeout: EditText
    private lateinit var virtualFloatModeMinimumBatteryVoltage: EditText
    private lateinit var lowBatteryVoltage: EditText
    private lateinit var criticalBatteryVoltage: EditText

    private lateinit var startOnBoot: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        protocol = findViewById(R.id.protocol)
        host = findViewById(R.id.hostname)
        port = findViewById(R.id.port)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        useAuth = findViewById(R.id.use_auth)
        generatorFloatHours = findViewById(R.id.generator_float_hours)
        initialRequestTimeout = findViewById(R.id.initial_request_timeout)
        subsequentRequestTimeout = findViewById(R.id.subsequent_request_timeout)
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
            notificationManager.deleteNotificationChannel("generator_done_notification") // this was used in a previous version but is not longer used

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
                })
            }
        }
        startServiceIfNotRunning(this)
    }
    fun saveSettings(view: View){
        saveSettings()
    }
    fun restartService(view: View){
        restartService(this)
    }
    fun stopService(view: View){
        stopService(this)
    }
    private fun saveSettings(){
        prefs.couchDb.protocol = protocol.text.toString()
        prefs.couchDb.host = host.text.toString()
        prefs.couchDb.port = port.text.toString().toIntOrNull() ?: DefaultOptions.CouchDb.port
        prefs.couchDb.username = username.text.toString()
        prefs.couchDb.password = password.text.toString()
        prefs.couchDb.useAuth = useAuth.isChecked

        prefs.generatorFloatTimeHours = generatorFloatHours.text.toString().toFloatOrNull() ?: DefaultOptions.generatorFloatTimeHours
        prefs.initialRequestTimeSeconds = initialRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.initialRequestTimeSeconds
        prefs.subsequentRequestTimeSeconds = subsequentRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.subsequentRequestTimeSeconds
        prefs.virtualFloatModeMinimumBatteryVoltage = virtualFloatModeMinimumBatteryVoltage.text.toString().toFloatOrNull() ?: DefaultOptions.virtualFloatModeMinimumBatteryVoltage
        prefs.lowBatteryVoltage = lowBatteryVoltage.text.toString().toFloatOrNull() ?: DefaultOptions.lowBatteryVoltage
        prefs.criticalBatteryVoltage = criticalBatteryVoltage.text.toString().toFloatOrNull() ?: DefaultOptions.criticalBatteryVoltage
        prefs.startOnBoot = startOnBoot.isChecked

        loadSettings()
        Toast.makeText(this, "Saved settings!", Toast.LENGTH_SHORT).show()
    }
    private fun loadSettings(){
        protocol.setText(prefs.couchDb.protocol)
        host.setText(prefs.couchDb.host)
        port.setText(prefs.couchDb.port.toString())
        username.setText(prefs.couchDb.username)
        password.setText(prefs.couchDb.password)
        useAuth.isChecked = prefs.couchDb.useAuth
        onUseAuthUpdate()

        generatorFloatHours.setText(prefs.generatorFloatTimeHours.toString())
        initialRequestTimeout.setText(prefs.initialRequestTimeSeconds.toString())
        subsequentRequestTimeout.setText(prefs.subsequentRequestTimeSeconds.toString())
        virtualFloatModeMinimumBatteryVoltage.setText(prefs.virtualFloatModeMinimumBatteryVoltage?.toString() ?: "")
        lowBatteryVoltage.setText(prefs.lowBatteryVoltage?.toString() ?: "")
        criticalBatteryVoltage.setText(prefs.criticalBatteryVoltage?.toString() ?: "")
        startOnBoot.isChecked = prefs.startOnBoot

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
}
