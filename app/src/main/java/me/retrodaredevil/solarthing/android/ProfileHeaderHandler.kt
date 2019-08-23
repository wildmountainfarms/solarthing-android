package me.retrodaredevil.solarthing.android

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.*
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import java.util.*

class ProfileHeaderHandler(
    private val context: Context,
    view: View,
    private val profileManager: ProfileManager<*>,
    private val doSave: (() -> Unit),
    private val doLoad: ((UUID) -> Unit)
)  {


    private val spinner: Spinner = view.findViewById(R.id.profile_spinner)
    private val profileNameEditText: EditText = view.findViewById(R.id.profile_name)
    private val profileUUIDMap = mutableMapOf<UUID, Pair<Long, String>>()

    init {
        val newButton = view.findViewById<Button>(R.id.new_profile_button)
        newButton.setOnClickListener(::newProfile)
        val deleteButton = view.findViewById<Button>(R.id.delete_profile_button)
        deleteButton.setOnClickListener(::deleteCurrentProfile)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = error("Cannot have nothing selected!")
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = onConnectionProfileChange(id)
        }
    }
    @Suppress("UNUSED_PARAMETER")
    fun newProfile(view: View){
        newProfilePrompt()
    }
    @Suppress("UNUSED_PARAMETER")
    fun deleteCurrentProfile(view: View){
        deleteCurrentProfile()
    }

    var profileName: String
        get() = profileNameEditText.text.toString()
        set(value) = profileNameEditText.setText(value)

    private fun onConnectionProfileChange(rowId: Long){
        println("Connection Profile Changed to ID: $rowId")
        var activeUUID: UUID? = null
        var activeName: String? = null
        for(entry in profileUUIDMap.entries){
            val uuid = entry.key
            val (id, name) = entry.value
            if(id == rowId){
                activeUUID = uuid
                activeName = name
                break
            }
        }
        activeUUID ?: error("activeUUID is null from rowId: $rowId")
        activeName!!
        val currentUUID = profileManager.activeUUID
        if(currentUUID == activeUUID){ // they're the same
            return
        }
//        saveSettings(reloadSettings = false, showToast = false)
        doSave()
        profileManager.activeUUID = activeUUID
//        loadConnectionSettings(activeName, connectionProfileManager.getProfile(activeUUID))
        doLoad(activeUUID)
        Toast.makeText(context, "Switched to connection profile: $activeName", Toast.LENGTH_SHORT).show()
    }

    /**
     * Used to reload the spinner values or/and to set the current selection
     * @param activeUUID The new active UUID to set
     */
    fun loadSpinner(activeUUID: UUID){
        val uuids = profileManager.profileUUIDs
        val profileNameList = uuids.map { profileManager.getProfileName(it) }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, profileNameList)
        var selectedPosition: Int? = null
        for((position, uuid) in uuids.withIndex()){
            val id = adapter.getItemId(position)
            profileUUIDMap[uuid] = Pair(id, profileNameList[position])
            if(uuid == activeUUID){
                selectedPosition = position
            }
        }
        selectedPosition ?: error("No active uuid: $activeUUID in uuids: $uuids")
        spinner.adapter = adapter
        spinner.setSelection(selectedPosition)
    }
    private fun newProfilePrompt(){
        createTextPromptAlert("New Profile Name") { name ->
            val (uuid, _) = profileManager.addAndCreateProfile(name)
            doSave()
            profileManager.activeUUID = uuid
            loadSpinner(uuid)
            doLoad(uuid)
            Toast.makeText(context, "New profile created!", Toast.LENGTH_SHORT).show()
            println("Created profile: $name")
        }.show()
    }
    private fun deleteCurrentProfile(){
        val size = profileManager.profileUUIDs.size
        if(size <= 1){
            Toast.makeText(context, "You cannot remove the last profile!", Toast.LENGTH_SHORT).show()
            return
        }
        createConfirmPromptAlert("Really Delete?") {
            val success = profileManager.removeProfile(profileManager.activeUUID)
            if (success) {
                val uuid = profileManager.activeUUID
                loadSpinner(uuid)
                doLoad(uuid)
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error. Unable to remove...", Toast.LENGTH_SHORT).show()
            }
        }.show()
    }
    private fun createTextPromptAlert(title: String, onSubmit: (String) -> Unit): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("OK"){ _, _ ->
            onSubmit(input.text.toString())
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        return builder.create()
    }
    private fun createConfirmPromptAlert(title: String, onSubmit: () -> Unit): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setPositiveButton("OK"){ _, _ ->
            onSubmit()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

}