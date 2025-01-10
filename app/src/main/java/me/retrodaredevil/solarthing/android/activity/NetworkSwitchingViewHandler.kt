package me.retrodaredevil.solarthing.android.activity

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.prefs.NetworkSwitchingProfile
import me.retrodaredevil.solarthing.android.util.SSIDNotAvailable
import me.retrodaredevil.solarthing.android.util.SSIDPermissionException
import me.retrodaredevil.solarthing.android.util.getSSID

class NetworkSwitchingViewHandler(
        private val context: Context,
        private val view: View,
        requestBackgroundLocation: () -> Unit
) {
    private val isEnabledCheckBox: CheckBox = view.findViewById(R.id.is_enabled)
    private val isBackupCheckBox: CheckBox = view.findViewById(R.id.is_backup)
    private val currentNetworkText: TextView = view.findViewById(R.id.current_network)
    private val bottomView: View = view.findViewById(R.id.network_switching_bottom)

    private var networkSSID: String? = null

    init {
        isEnabledCheckBox.setOnCheckedChangeListener { _, _ ->
            updateBottomVisibility()
        }
        isBackupCheckBox.setOnCheckedChangeListener { _, _ ->
            updateBottomVisibility()
        }
        view.findViewById<Button>(R.id.set_to_this_network).setOnClickListener {
            try {
                networkSSID = getSSID(context)
                updateCurrentNetwork()
            } catch (ex: SSIDPermissionException){
                requestBackgroundLocation()
            } catch(ex: SSIDNotAvailable){
                Toast.makeText(context, "SSID currently not available. Is location enabled?", Toast.LENGTH_LONG).show()
            }
        }
    }
    fun show(show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun getNetworkSwitchingProfile() = NetworkSwitchingProfile(isEnabledCheckBox.isChecked, isBackupCheckBox.isChecked, networkSSID)

    fun load(profile: NetworkSwitchingProfile){
        val isEnabled = profile.isEnabled
        val isBackup = profile.isBackup
        isEnabledCheckBox.isChecked = isEnabled
        isBackupCheckBox.isChecked = isBackup
        updateBottomVisibility()
        networkSSID = profile.ssid
        updateCurrentNetwork()
    }

    private fun updateBottomVisibility(){
        if(isBackupCheckBox.isChecked || !isEnabledCheckBox.isChecked){
            hideBottom()
        } else {
            showBottom()
        }
    }
    private fun hideBottom(){
        bottomView.visibility = View.GONE
    }
    private fun showBottom(){
        bottomView.visibility = View.VISIBLE
    }
    private fun updateCurrentNetwork(){
        currentNetworkText.text = with(networkSSID){
            if(this == null){
                "Off WiFi"
            } else {
                "SSID: $this"
            }
        }
    }

}
