package me.retrodaredevil.solarthing.android.activity

import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import me.retrodaredevil.solarthing.android.StartupHelper
import me.retrodaredevil.solarthing.android.components.RequestPostNotificationsComponent
import me.retrodaredevil.solarthing.android.service.PersistentService
import me.retrodaredevil.solarthing.android.util.DrawerHandler
import me.retrodaredevil.solarthing.android.util.initializeDrawer

class MainActivity : AppCompatActivity() {

    private lateinit var drawerHandler: DrawerHandler
    private lateinit var requestPostNotificationsComponent: RequestPostNotificationsComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(me.retrodaredevil.solarthing.android.R.layout.activity_main)
        val welcomeText = findViewById<TextView>(me.retrodaredevil.solarthing.android.R.id.welcomeTextView);
        drawerHandler = initializeDrawer(this)
        requestPostNotificationsComponent = RequestPostNotificationsComponent.createAndRegister(this)

        emptyList<Any>().javaClass.declaredMethods // put this here to make sure app doesn't crash // This will make app crash on old android versions with core library desugaring enabled

        StartupHelper(this).onStartup()
        // Thanks https://stackoverflow.com/a/38272292/5434860
        val welcomeTextHtmlString = getString(me.retrodaredevil.solarthing.android.R.string.welcome_text)
        welcomeText.text = Html.fromHtml(welcomeTextHtmlString, Html.FROM_HTML_MODE_COMPACT);
    }
    override fun onResume() {
        super.onResume()
        drawerHandler.closeDrawer()
        drawerHandler.highlight()
    }
    @Suppress("UNUSED_PARAMETER")
    fun restartService(view: View){
        if (requestPostNotificationsComponent.requestNotificationPermission()) {
            PersistentService.serviceHelper.restartService(this)
        } else {
            Toast.makeText(this, "Please enable notifications to utilize this feature!", Toast.LENGTH_SHORT).show()
        }
    }
    @Suppress("UNUSED_PARAMETER")
    fun stopService(view: View){
        PersistentService.serviceHelper.stopService(this)
    }
    @Suppress("UNUSED_PARAMETER")
    fun enableNotifications(view: View){
        if (requestPostNotificationsComponent.requestNotificationPermission()) {
            Toast.makeText(this, "Notifications already enabled!", Toast.LENGTH_SHORT).show()
        }
    }
}
