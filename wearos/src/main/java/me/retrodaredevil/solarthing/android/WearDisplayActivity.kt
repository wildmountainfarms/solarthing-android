package me.retrodaredevil.solarthing.android

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport

class WearDisplayActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear_display)

        // Enables Always-on
        AmbientModeSupport.attach(this)
//        val isAmbient = controller.isAmbient
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {

        }
    }
}
