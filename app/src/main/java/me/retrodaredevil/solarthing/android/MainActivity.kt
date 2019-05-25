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
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels

class MainActivity : AppCompatActivity() {

    private val prefs = Prefs(this)

    private lateinit var databaseName: EditText
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        databaseName = findViewById(R.id.database_name)
        protocol = findViewById(R.id.protocol)
        host = findViewById(R.id.hostname)
        port = findViewById(R.id.port)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        useAuth = findViewById(R.id.use_auth)
        generatorFloatHours = findViewById(R.id.generator_float_hours)
        initialRequestTimeout = findViewById(R.id.initial_request_timeout)
        subsequentRequestTimeout = findViewById(R.id.subsequent_request_timeout)
        virtualFloatModeMinimumBatteryVoltage = findViewById(R.id.virtual_float_mode_minimum_battery_voltage)

        useAuth.setOnCheckedChangeListener{ _, _ ->
            onUseAuthUpdate()
        }

        loadSettings()


        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                })
            }
        }
        me.retrodaredevil.solarthing.android.service.restartService(this)
    }
    fun saveSettings(view: View){
        saveSettings()
    }
    fun restartService(view: View){
        me.retrodaredevil.solarthing.android.service.restartService(this)
    }
    fun stopService(view: View){
        me.retrodaredevil.solarthing.android.service.stopService(this)
    }
    private fun saveSettings(){
        prefs.couchDb.databaseName = databaseName.text.toString()
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

        loadSettings()
    }
    private fun loadSettings(){
        databaseName.setText(prefs.couchDb.databaseName)
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
