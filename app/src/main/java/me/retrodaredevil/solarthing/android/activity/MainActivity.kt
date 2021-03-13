package me.retrodaredevil.solarthing.android.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.StartupHelper
import me.retrodaredevil.solarthing.android.service.restartService
import me.retrodaredevil.solarthing.android.service.stopService
import me.retrodaredevil.solarthing.android.util.DrawerHandler
import me.retrodaredevil.solarthing.android.util.initializeDrawer

class MainActivity : AppCompatActivity() {

    private lateinit var drawerHandler: DrawerHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerHandler = initializeDrawer(this)

        emptyList<Any>().javaClass.declaredMethods // put this here to make sure app doesn't crash // This will make app crash on old android versions with core library desugaring enabled


        StartupHelper(this).onStartup()

    }
    override fun onResume() {
        super.onResume()
        drawerHandler.closeDrawer()
        drawerHandler.highlight()
    }
    @Suppress("UNUSED_PARAMETER")
    fun restartService(view: View){
        restartService(this)
    }
    @Suppress("UNUSED_PARAMETER")
    fun stopService(view: View){
        stopService(this)
    }
}