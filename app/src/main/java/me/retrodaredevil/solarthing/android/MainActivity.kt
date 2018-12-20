package me.retrodaredevil.solarthing.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
        val properties = CouchDbProperties(databaseName.text.toString(), false, protocol.text.toString(),
            host.text.toString(), port.text.toString().toIntOrNull() ?: 5984, username.text.toString(), password.text.toString())
        GlobalData.connectionProperties = properties
        getSharedPreferences("connection_properties", 0).edit().apply {
            putString("database_name", properties.dbName)
            putString("protocol", properties.protocol)
            putString("host", properties.host)
            putInt("port", properties.port)
            putString("username", properties.username)
            putString("password", properties.password)
            apply()
        }

        getSharedPreferences("settings", 0).edit().apply {
            val hours = generatorFloatHours.text.toString().toFloatOrNull() ?: 1.5F
            putFloat("generator_float_hours", hours)
            apply()
            GlobalData.generatorFloatTimeMillis = (hours * 60 * 60 * 1000).toLong()
        }

    }
    private fun loadSettings(){
        getSharedPreferences("connection_properties", 0).apply {
            val connectionProperties = CouchDbProperties(getString("database_name", "solarthing"), false,
                getString("protocol", "http"),
                getString("host", "192.168.1.203"), getInt("port", 5984),
                getString("username", "admin"), getString("password", "relax"))
            databaseName.setText(connectionProperties.dbName)
            protocol.setText(connectionProperties.protocol)
            host.setText(connectionProperties.host)
            port.setText(connectionProperties.port.toString())
            username.setText(connectionProperties.username)
            password.setText(connectionProperties.password)

            GlobalData.connectionProperties = connectionProperties
        }
        getSharedPreferences("settings", 0).apply {
            val hours = getFloat("generator_float_hours", -1F)
            if(hours >= 0){
                GlobalData.generatorFloatTimeMillis = (hours * 60 * 60 * 1000).toLong()
                generatorFloatHours.setText(hours.toString())
            } else {
                val storedHours: Double = GlobalData.generatorFloatTimeMillis / (1000.0 * 60 * 60)
                generatorFloatHours.setText(storedHours.toString())
            }
        }
    }
}
