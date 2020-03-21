package me.retrodaredevil.solarthing.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class EventDisplayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_display)
        initializeDrawer(this)
    }
}
