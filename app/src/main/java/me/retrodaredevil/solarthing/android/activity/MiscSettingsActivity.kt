package me.retrodaredevil.solarthing.android.activity

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import me.retrodaredevil.solarthing.android.StartupHelper
import me.retrodaredevil.solarthing.android.createMiscProfileProvider
import me.retrodaredevil.solarthing.android.data.TemperatureUnit
import me.retrodaredevil.solarthing.android.prefs.DefaultOptions
import me.retrodaredevil.solarthing.android.prefs.MiscProfile
import me.retrodaredevil.solarthing.android.prefs.ProfileHolder
import me.retrodaredevil.solarthing.android.prefs.ProfileProvider
import me.retrodaredevil.solarthing.android.util.DrawerHandler
import me.retrodaredevil.solarthing.android.util.initializeDrawerWithUnsavedPrompt

class MiscSettingsActivity : AppCompatActivity() {

    private lateinit var miscProfileProvider: ProfileProvider<MiscProfile>

    private lateinit var maxFragmentTime: EditText
    private lateinit var startOnBoot: CheckBox
    private lateinit var networkSwitchingEnabledCheckBox: CheckBox
    private lateinit var temperatureUnitSpinner: Spinner
    private lateinit var enableWearOsSupport: CheckBox

    private lateinit var drawerHandler: DrawerHandler

    // region Initialization
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(me.retrodaredevil.solarthing.android.R.layout.activity_settings_other)
        emptyList<Any>().javaClass.declaredMethods

        miscProfileProvider = createMiscProfileProvider(this)

        maxFragmentTime = findViewById(me.retrodaredevil.solarthing.android.R.id.max_fragment_time)
        startOnBoot = findViewById(me.retrodaredevil.solarthing.android.R.id.start_on_boot)
        networkSwitchingEnabledCheckBox = findViewById(me.retrodaredevil.solarthing.android.R.id.network_switching_enabled)
        temperatureUnitSpinner = findViewById(me.retrodaredevil.solarthing.android.R.id.temperature_unit_spinner)
        enableWearOsSupport = findViewById(me.retrodaredevil.solarthing.android.R.id.wear_os_support)


        drawerHandler = initializeDrawerWithUnsavedPrompt(
                this,
                isNotSavedGetter = { isMiscProfileNotSaved() },
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
    // endregion

    // region Saving and Loading
    private fun saveSettings(reloadSettings: Boolean = true, showToast: Boolean = true){
        miscProfileProvider.activeProfile.profile = getMiscProfile()

        if(reloadSettings) loadSettings()
        if(showToast) Toast.makeText(this, "Saved settings!", Toast.LENGTH_SHORT).show()
    }
    private fun getMiscProfile(): MiscProfile {
        val position = temperatureUnitSpinner.selectedItemPosition
        val temperatureUnit = if(position != AdapterView.INVALID_POSITION) TemperatureUnit.values()[position] else DefaultOptions.temperatureUnit
        return MiscProfile(
                maxFragmentTime.text.toString().toFloatOrNull() ?: DefaultOptions.maxFragmentTimeMinutes,
                startOnBoot.isChecked,
                networkSwitchingEnabledCheckBox.isChecked,
                temperatureUnit,
                enableWearOsSupport.isChecked
        )
    }
    private fun isMiscProfileNotSaved() = getMiscProfile() != miscProfileProvider.activeProfile.profile
    private fun loadSettings(){
        loadMiscSettings(miscProfileProvider.activeProfile)
    }
    private fun loadMiscSettings(profileHolder: ProfileHolder<MiscProfile>) {
        profileHolder.profile.let {
            maxFragmentTime.setText(it.maxFragmentTimeMinutes.toString())
            startOnBoot.isChecked = it.startOnBoot
            networkSwitchingEnabledCheckBox.isChecked = it.networkSwitchingEnabled

            temperatureUnitSpinner.let { spinner ->
                val array = TemperatureUnit.values()
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, getNiceTemperatureUnitStringList(array))
                val temperatureUnit = it.temperatureUnit
                var selection: Int? = null
                for((i, type) in array.withIndex()){
                    if(type == temperatureUnit){
                        selection = i
                    }
                }
                selection!!
                spinner.setSelection(selection)
            }
            enableWearOsSupport.isChecked = it.enableWearOsSupport
        }
    }
    // endregion
    private fun getNiceTemperatureUnitStringList(values: Array<TemperatureUnit>): List<String> {
        return values.map {
            when(it) {
                TemperatureUnit.FAHRENHEIT -> "Fahrenheit"
                TemperatureUnit.CELSIUS -> "Celsius"
            }
        }
    }
}