package me.retrodaredevil.solarthing.android.activity

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import me.retrodaredevil.solarthing.android.SettingsTemperatureNotifyHandler
import me.retrodaredevil.solarthing.android.StartupHelper
import me.retrodaredevil.solarthing.android.createMiscProfileProvider
import me.retrodaredevil.solarthing.android.createSolarProfileManager
import me.retrodaredevil.solarthing.android.prefs.BatteryVoltageType
import me.retrodaredevil.solarthing.android.prefs.DefaultOptions
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import me.retrodaredevil.solarthing.android.prefs.SolarProfile
import me.retrodaredevil.solarthing.android.util.DrawerHandler
import me.retrodaredevil.solarthing.android.util.initializeDrawerWithUnsavedPrompt
import java.util.*

class SolarSettingsActivity : AppCompatActivity() {
    private val temperatureNotifyHandler = SettingsTemperatureNotifyHandler(this)

    private lateinit var solarProfileManager: ProfileManager<SolarProfile>
    private lateinit var solarProfileHeader: ProfileHeaderHandler


    private lateinit var lowBatteryVoltage: EditText
    private lateinit var criticalBatteryVoltage: EditText
    private lateinit var batteryVoltageTypeSpinner: Spinner


    private lateinit var drawerHandler: DrawerHandler

    // region Initialization
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(me.retrodaredevil.solarthing.android.R.layout.activity_settings_solar)
        emptyList<Any>().javaClass.declaredMethods



        solarProfileManager = createSolarProfileManager(this)
        solarProfileHeader = ProfileHeaderHandler(
                this,
                findViewById(me.retrodaredevil.solarthing.android.R.id.solar_profile_header_layout),
                solarProfileManager,
                this::saveSolarSettings,
                this::loadSolarSettings
        )

        lowBatteryVoltage = findViewById(me.retrodaredevil.solarthing.android.R.id.low_battery_voltage)
        criticalBatteryVoltage = findViewById(me.retrodaredevil.solarthing.android.R.id.critical_battery_voltage)
        batteryVoltageTypeSpinner = findViewById(me.retrodaredevil.solarthing.android.R.id.battery_type_spinner)

        drawerHandler = initializeDrawerWithUnsavedPrompt(
                this,
                isNotSavedGetter = { isSolarProfileNotSaved() },
                saveSettings = { saveSettings(false, true) }
        )

        loadSettings()

        StartupHelper(this).onStartup()
    }

    override fun onResume() {
        super.onResume()
        drawerHandler.closeDrawer()
        drawerHandler.highlight()
    }

    // region Buttons
    @Suppress("UNUSED_PARAMETER")
    fun saveSettings(view: View){
        saveSettings()
    }
    fun openTemperatureNotifySettings(@Suppress("UNUSED_PARAMETER") view: View){
        val miscProfileProvider = createMiscProfileProvider(this)
        val miscProfile = miscProfileProvider.activeProfile.profile
        temperatureNotifyHandler.showDialog(miscProfile.temperatureUnit)
    }
    // endregion
    // region Saving and Loading
    private fun saveSettings(reloadSettings: Boolean = true, showToast: Boolean = true){
        saveSolarSettings(solarProfileHeader.editUUID)

        if(reloadSettings) loadSettings()
        if(showToast) Toast.makeText(this, "Saved settings!", Toast.LENGTH_SHORT).show()
    }
    private fun getSolarProfile(): SolarProfile {
        val position = batteryVoltageTypeSpinner.selectedItemPosition
        val batteryVoltageType = if(position != AdapterView.INVALID_POSITION){
            BatteryVoltageType.values()[position]
        } else {
            DefaultOptions.batteryVoltageType
        }
        return SolarProfile(
                Collections.emptyList(), temperatureNotifyHandler.getTemperatureNodesToSave(),
                lowBatteryVoltage.text.toString().toFloatOrNull(),
                criticalBatteryVoltage.text.toString().toFloatOrNull(),
                batteryVoltageType
        )
    }
    private fun isSolarProfileNotSaved() = getSolarProfile() != solarProfileManager.getProfile(solarProfileHeader.editUUID).profile
    private fun saveSolarSettings(uuid: UUID){
        solarProfileManager.setProfileName(uuid, solarProfileHeader.profileName)
        solarProfileManager.getProfile(uuid).profile = getSolarProfile()
    }
    private fun loadSettings(){
        solarProfileHeader.editUUID.let {
            loadSolarSettings(it)
            solarProfileHeader.loadSpinner(it)
        }
    }
    private fun loadSolarSettings(uuid: UUID) {
        val profile = solarProfileManager.getProfile(uuid).profile
        val name = solarProfileManager.getProfileName(uuid)
        solarProfileHeader.profileName = name
        profile.let {
            lowBatteryVoltage.setText(it.lowBatteryVoltage?.toString() ?: "")
            criticalBatteryVoltage.setText(it.criticalBatteryVoltage?.toString() ?: "")

            batteryVoltageTypeSpinner.let { spinner ->
                val array = BatteryVoltageType.values()
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, getNiceBatteryVoltageStringList(array))
                val batteryVoltageType = it.batteryVoltageType
                var selection: Int? = null
                for((i, type) in array.withIndex()){
                    if(type == batteryVoltageType){
                        selection = i
                    }
                }
                selection!!
                spinner.setSelection(selection)
            }
            temperatureNotifyHandler.loadTemperatureNodes(it.temperatureNodes)
        }
    }
    // endregion
    private fun getNiceBatteryVoltageStringList(values: Array<BatteryVoltageType>): List<String> {
        return values.map {
            when(it){
                BatteryVoltageType.AVERAGE -> "Average"
                BatteryVoltageType.FIRST_PACKET -> "First packet"
                BatteryVoltageType.MOST_RECENT -> "Most recent"
                BatteryVoltageType.FIRST_OUTBACK -> "First Outback"
                BatteryVoltageType.FIRST_OUTBACK_FX -> "First Outback FX"
            }
        }
    }
}