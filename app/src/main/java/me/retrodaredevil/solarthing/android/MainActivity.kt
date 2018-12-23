package me.retrodaredevil.solarthing.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import org.lightcouch.CouchDbProperties

class MainActivity : AppCompatActivity() {

    private lateinit var databaseName: EditText
    private lateinit var protocol: EditText
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var generatorFloatHours: EditText
    private lateinit var initialRequestTimeout: EditText
    private lateinit var subsequentRequestTimeout: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        databaseName = findViewById(R.id.database_name)
        protocol = findViewById(R.id.protocol)
        host = findViewById(R.id.hostname)
        port = findViewById(R.id.port)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        generatorFloatHours = findViewById(R.id.generator_float_hours)
        initialRequestTimeout = findViewById(R.id.initial_request_timeout)
        subsequentRequestTimeout = findViewById(R.id.subsequent_request_timeout)

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
                })
            }
        }
        restartService()
    }
    fun saveSettings(view: View){
        saveSettings()
    }
    fun restartService(view: View){
        restartService()
    }
    fun stopService(view: View){
        stopService()
    }
    private fun restartService(){
        val serviceIntent = Intent(this, PersistentService::class.java)
        stopService(serviceIntent)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    private fun stopService(){
        val serviceIntent = Intent(this, PersistentService::class.java)
        stopService(serviceIntent)
    }
    private fun saveSettings(){
        getSharedPreferences("connection_properties", 0).edit().apply {
            putString("database_name", databaseName.text.toString())
            putString("protocol", protocol.text.toString())
            putString("host", host.text.toString())
            putInt("port", port.text.toString().toIntOrNull() ?: 5984)
            putString("username", username.text.toString())
            putString("password", password.text.toString())
            apply()
        }

        getSharedPreferences("settings", 0).edit().apply {
            val hours = generatorFloatHours.text.toString().toFloatOrNull() ?: DefaultOptions.generatorFloatTimeHours
            val initialSeconds = initialRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.initialRequestTimeSeconds
            val subsequentSeconds = subsequentRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.subsequentRequestTimeSeconds
            putFloat("generator_float_hours", hours)
            putInt("initial_request_timeout", initialSeconds)
            putInt("subsequent_request_timeout", subsequentSeconds)
            apply()
        }
        loadSettings()
    }
    private fun loadSettings(){
        getSharedPreferences("connection_properties", 0).apply {
            val connectionProperties = CouchDbProperties(
                getString("database_name", DefaultOptions.CouchDb.databaseName),
                false,
                getString("protocol", DefaultOptions.CouchDb.protocol),
                getString("host", DefaultOptions.CouchDb.host),
                getInt("port", DefaultOptions.CouchDb.port),
                getString("username", DefaultOptions.CouchDb.username),
                getString("password", DefaultOptions.CouchDb.password)
            )
            databaseName.setText(connectionProperties.dbName)
            protocol.setText(connectionProperties.protocol)
            host.setText(connectionProperties.host)
            port.setText(connectionProperties.port.toString())
            username.setText(connectionProperties.username)
            password.setText(connectionProperties.password)

            GlobalData.connectionProperties = connectionProperties
        }
        getSharedPreferences("settings", 0).apply {
            val hours = getFloat("generator_float_hours", DefaultOptions.generatorFloatTimeHours)
            GlobalData.generatorFloatTimeHours = hours
            generatorFloatHours.setText(hours.toString())

            val initialSeconds = getInt("initial_request_timeout", DefaultOptions.initialRequestTimeSeconds)
            GlobalData.initialRequestTimeSeconds = initialSeconds
            initialRequestTimeout.setText(initialSeconds.toString())

            val subsequentSeconds = getInt("subsequent_request_timeout", DefaultOptions.subsequentRequestTimeSeconds)
            GlobalData.subsequentRequestTimeSeconds = subsequentSeconds
            subsequentRequestTimeout.setText(subsequentSeconds.toString())
        }
    }
}
