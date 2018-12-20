package me.retrodaredevil.solarthing.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import org.lightcouch.CouchDbProperties
import java.util.concurrent.ScheduledThreadPoolExecutor

class MainActivity : AppCompatActivity() {

    private lateinit var databaseName: EditText
    private lateinit var protocol: EditText
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        databaseName = findViewById(R.id.database_name)
        protocol = findViewById(R.id.protocol)
        host = findViewById(R.id.hostname)
        port = findViewById(R.id.port)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)

        loadConnectionProperties()


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
    fun saveConnectionProperties(view: View){
        saveConnectionProperties()
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
    private fun saveConnectionProperties(){
        val properties = CouchDbProperties(databaseName.text.toString(), false, protocol.text.toString(),
            host.text.toString(), port.text.toString().toInt(), username.text.toString(), password.text.toString())
        GlobalData.connectionProperties = properties
        val settings = this.getSharedPreferences("connection_properties", 0)

        val editor = settings.edit()
        editor.putString("database_name", properties.dbName)
        editor.putString("protocol", properties.protocol)
        editor.putString("host", properties.host)
        editor.putInt("port", properties.port)
        editor.putString("username", properties.username)
        editor.putString("password", properties.password)
        editor.apply()
    }
    private fun loadConnectionProperties(){
        val settings = this.getSharedPreferences("connection_properties", 0)
        val connectionProperties = CouchDbProperties(settings.getString("database_name", "solarthing"), false,
            settings.getString("protocol", "http"),
            settings.getString("host", "192.168.1.203"), settings.getInt("port", 5984),
            settings.getString("username", "admin"), settings.getString("password", "relax"))
        databaseName.setText(connectionProperties.dbName)
        protocol.setText(connectionProperties.protocol)
        host.setText(connectionProperties.host)
        port.setText(connectionProperties.port.toString())
        username.setText(connectionProperties.username)
        password.setText(connectionProperties.password)

        GlobalData.connectionProperties = connectionProperties
    }
}
