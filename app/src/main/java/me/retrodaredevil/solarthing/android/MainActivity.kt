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

    private val prefs = Prefs(this)

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
        prefs.couchDbProperties = CouchDbProperties(
            databaseName.text.toString(),
            false,
            protocol.text.toString(),
            host.text.toString(),
            port.text.toString().toIntOrNull() ?: DefaultOptions.CouchDb.port,
            username.text.toString(),
            password.text.toString()
        )

        prefs.generatorFloatTimeHours = generatorFloatHours.text.toString().toFloatOrNull() ?: DefaultOptions.generatorFloatTimeHours
        prefs.initialRequestTimeSeconds = initialRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.initialRequestTimeSeconds
        prefs.subsequentRequestTimeSeconds = subsequentRequestTimeout.text.toString().toIntOrNull() ?: DefaultOptions.subsequentRequestTimeSeconds

        loadSettings()
    }
    private fun loadSettings(){
        val connectionProperties = prefs.couchDbProperties
        databaseName.setText(connectionProperties.dbName)
        protocol.setText(connectionProperties.protocol)
        host.setText(connectionProperties.host)
        port.setText(connectionProperties.port.toString())
        username.setText(connectionProperties.username)
        password.setText(connectionProperties.password)

        generatorFloatHours.setText(prefs.generatorFloatTimeHours.toString())
        initialRequestTimeout.setText(prefs.initialRequestTimeSeconds.toString())
        subsequentRequestTimeout.setText(prefs.subsequentRequestTimeSeconds.toString())

    }
}
