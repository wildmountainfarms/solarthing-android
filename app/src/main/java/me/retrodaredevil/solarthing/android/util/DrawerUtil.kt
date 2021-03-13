package me.retrodaredevil.solarthing.android.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.activity.*
import me.retrodaredevil.solarthing.android.createConnectionProfileManager
import me.retrodaredevil.solarthing.android.prefs.CouchDbDatabaseConnectionProfile


/*
Thanks https://android.jlelse.eu/android-using-navigation-drawer-across-multiple-activities-the-easiest-way-b011f152aebd
and of course thanks https://github.com/mikepenz/MaterialDrawer
 */

class DrawerHandler(
        private val itemIdentifier: Long,
        private val drawer: Drawer
) {
    fun closeDrawer() = drawer.closeDrawer()
    fun highlight() = drawer.setSelection(itemIdentifier, false)
}

fun initializeDrawer(
        activity: Activity,
        toolbar: Toolbar = activity.findViewById(R.id.toolbar),
        onActivityIntentRequest: (View, Intent, DrawerHandler) -> Unit = { view, intent, _ ->
            view.context.startActivity(intent)
            activity.finish()
        }
) : DrawerHandler {
    val drawerEmptyItem = PrimaryDrawerItem().withIdentifier(0).withName("")

    val drawerItemMain: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(200)
            .withName(R.string.main_select)

    val drawerItemSettingsConnection: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(1)
            .withName(R.string.settings_connection_select)
    val drawerItemSettingsSolar: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(2)
            .withName(R.string.settings_solar_select)
    val drawerItemSettingsMisc: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(3)
            .withName(R.string.settings_misc_select)
    val drawerItemEventDisplay: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(4)
            .withName(R.string.event_display_select)
    val drawerItemCommands: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(5)
            .withName(R.string.commands_select)

    val drawerNotificationSettings: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(100)
            .withName(R.string.notification_settings_select)
    val drawerApplicationSettings: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(101)
            .withName(R.string.application_settings_select)
    val drawerPrivacyPolicy: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(103)
            .withName(R.string.privacy_policy_select)

    val itemIdentifier: Long = when(activity){
        is MainActivity -> 200
        is ConnectionSettingsActivity -> 1
        is SolarSettingsActivity -> 2
        is MiscSettingsActivity -> 3
        is EventDisplayActivity -> 4
        is CommandActivity -> 5
        else -> -1
    }
    val drawerHandlerHolder = arrayOf<DrawerHandler?>(null)
    val drawer = DrawerBuilder()
            .withActivity(activity)
            .withToolbar(toolbar)
            .withActionBarDrawerToggle(true)
            .withActionBarDrawerToggleAnimated(true)
            .withCloseOnClick(true)
            .withSelectedItem(itemIdentifier)
            .addDrawerItems(
                    drawerEmptyItem,
                    drawerItemMain,
                    DividerDrawerItem(),
                    drawerItemSettingsConnection,
                    drawerItemSettingsSolar,
                    drawerItemSettingsMisc,
                    DividerDrawerItem(),
                    drawerItemEventDisplay,
                    drawerItemCommands,
                    DividerDrawerItem(),
                    drawerNotificationSettings,
                    drawerApplicationSettings,
                    drawerPrivacyPolicy,
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                    view!!
                    val drawerHandler = drawerHandlerHolder[0]!!
                    when(drawerItem.identifier){
                        200L -> launchActivity<MainActivity>(view, activity, drawerHandler, onActivityIntentRequest)
                        1L -> launchActivity<ConnectionSettingsActivity>(view, activity, drawerHandler, onActivityIntentRequest)
                        2L -> launchActivity<SolarSettingsActivity>(view, activity, drawerHandler, onActivityIntentRequest)
                        3L -> launchActivity<MiscSettingsActivity>(view, activity, drawerHandler, onActivityIntentRequest)
                        4L -> launchActivity<EventDisplayActivity>(view, activity, drawerHandler, onActivityIntentRequest)
                        5L -> launchActivity<CommandActivity>(view, activity, drawerHandler, onActivityIntentRequest)
                        100L -> {
                            // credit here: https://stackoverflow.com/a/45192258/5434860
                            val intent = Intent()
                            when {
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                                    intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                                }
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                                    intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                    intent.putExtra("app_package", activity.packageName)
                                    intent.putExtra("app_uid", activity.applicationInfo.uid)
                                }
                                else -> {
                                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                                    intent.data = Uri.parse("package:" + activity.packageName)
                                }
                            }
                            activity.startActivity(intent)
                        }
                        101L -> {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", activity.packageName, null)
                            }
                            activity.startActivity(intent)
                        }
                        103L -> {
                            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wildmountainfarms/solarthing-android/blob/master/privacy_policy.md")))
                        }
                    }
                    return true
                }
            })
            .build()
    val r = DrawerHandler(itemIdentifier, drawer)
    drawerHandlerHolder[0] = r
    return r
}
private inline fun <reified T> launchActivity(view: View, currentActivity: Activity, drawerHandler: DrawerHandler, onActivityIntentRequest: (View, Intent, DrawerHandler) -> Unit) {
    if(currentActivity is T){
        return
    }
    val intent = Intent(currentActivity, T::class.java)
    onActivityIntentRequest(view, intent, drawerHandler)
}

fun initializeDrawerWithUnsavedPrompt(activity: Activity, toolbar: Toolbar = activity.findViewById(R.id.toolbar), isNotSavedGetter: () -> Boolean, saveSettings: () -> Unit) : DrawerHandler {
    return initializeDrawer(activity, toolbar) { _, intent, drawerHandler ->
        if (isNotSavedGetter()) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("You have unsaved changes")
            val input = EditText(activity)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)
            builder.setPositiveButton("Save") { _, _ ->
                saveSettings()
                activity.startActivity(intent)
                activity.finish()
            }
            builder.setNegativeButton("Don't Save") { _, _ ->
                activity.startActivity(intent)
                activity.finish()
            }
            builder.setNeutralButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            drawerHandler.highlight() // We aren't changing to new activity yet, so highlight current one
            builder.create().show()
        } else {
            activity.startActivity(intent)
            activity.finish()
        }
    }
}
