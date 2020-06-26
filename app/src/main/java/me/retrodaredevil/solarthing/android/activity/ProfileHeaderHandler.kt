package me.retrodaredevil.solarthing.android.activity

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.*
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.prefs.ProfileManager
import java.util.*

/**
 * Code for the "profile header". This is used in the settings activity.
 */
class ProfileHeaderHandler(
        private val context: Context,
        view: View,
        private val profileManager: ProfileManager<*>,
        private val doSave: (UUID) -> Unit,
        private val doLoad: (UUID) -> Unit
)  {


    private val spinner: Spinner = view.findViewById(R.id.profile_spinner)
    private val profileNameEditText: EditText = view.findViewById(R.id.profile_name)

    /**
     * The [UUID] of the profile being edited. Not necessarily the active uuid
     */
    var editUUID: UUID = profileManager.activeUUID

    init {
        val newButton = view.findViewById<Button>(R.id.new_profile_button)
        newButton.setOnClickListener(::newProfile)
        val deleteButton = view.findViewById<Button>(R.id.delete_profile_button)
        deleteButton.setOnClickListener(::deleteCurrentProfile)

        view.findViewById<Button>(R.id.active_profile_button).setOnClickListener(::activateProfile)
    }
    private fun activateProfile(view: View){
        val previous = profileManager.activeUUID
        val newUUID = editUUID
        if(previous != newUUID){
            doSave(newUUID) // we want to write possible changes so other callers can see the changes
            profileManager.activeUUID = newUUID
            Toast.makeText(context, "Switched to profile: ${profileManager.getProfileName(newUUID)}", Toast.LENGTH_SHORT).show()
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

    /**
     * Used to reload the spinner values or/and to set the current selection
     * @param newEditUUID The new UUID of the profile to edit
     */
    fun loadSpinner(newEditUUID: UUID){
        this.editUUID = newEditUUID
        val uuids = profileManager.profileUUIDs
        val profileNameList = uuids.map { profileManager.getProfileName(it) }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, profileNameList)
        var selectedPosition: Int? = null
        for((position, uuid) in uuids.withIndex()){
            if(uuid == newEditUUID){
                selectedPosition = position
            }
        }
        selectedPosition ?: error("No uuid: $newEditUUID in uuids: $uuids")
        spinner.adapter = adapter
        spinner.setSelection(selectedPosition)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = error("Cannot have nothing selected!")
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedUUID: UUID = uuids[position]
                val currentUUID = this@ProfileHeaderHandler.editUUID
                if(currentUUID == selectedUUID){ // they're the same
                    return
                }
                doSave(currentUUID) // we need to tell the UI to save the current settings because we're about to change the profile being edited profile
                this@ProfileHeaderHandler.editUUID = selectedUUID
                doLoad(selectedUUID) // we need to tell the UI to load the new settings
                println("Connection Profile Changed to position: $position uuid: $selectedUUID")
            }
        }

    }
    private fun newProfilePrompt(){
        createTextPromptAlert("New Profile Name") { name ->
            val (uuid, _) = profileManager.addAndCreateProfile(name)
            doSave(editUUID) // we need to tell the UI to save the profile because we're about to change the active profile
            editUUID = uuid
            loadSpinner(uuid) // reload the spinner and set its active profile
            doLoad(uuid) // we need to tell the UI to load the new profile
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
            val success = profileManager.removeProfile(editUUID) // remove the profile we're editing
            if (success) {
                val uuid = profileManager.activeUUID // now the new profile we're editing is the one that's currently active
                loadSpinner(uuid)
                doLoad(uuid) // we need to tell the UI to load the settings on the now active profile
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