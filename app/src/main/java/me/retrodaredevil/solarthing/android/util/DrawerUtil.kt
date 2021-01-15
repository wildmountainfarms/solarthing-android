package me.retrodaredevil.solarthing.android.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.activity.CommandActivity
import me.retrodaredevil.solarthing.android.activity.EventDisplayActivity
import me.retrodaredevil.solarthing.android.activity.SettingsActivity
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
        onActivityIntentRequest: (View, Intent) -> Unit = { view, intent -> view.context.startActivity(intent) }
) : DrawerHandler {
    val drawerEmptyItem = PrimaryDrawerItem().withIdentifier(0).withName("")

    val drawerItemSettings: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(1)
            .withName(R.string.settings_select)
    val drawerItemEventDisplay: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(2)
            .withName(R.string.event_display_select)
    val drawerItemCommands: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(3)
            .withName(R.string.commands_select)

    val drawerNotificationSettings: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(100)
            .withName(R.string.notification_settings_select)
    val drawerApplicationSettings: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(101)
            .withName(R.string.application_settings_select)
    val drawerGrafana: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(102)
            .withName(R.string.grafana_select)
    val drawerPrivacyPolicy: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(103)
            .withName(R.string.privacy_policy_select)

    val itemIdentifier: Long = when(activity){
        is SettingsActivity -> 1
        is EventDisplayActivity -> 2
        is CommandActivity -> 3
        else -> -1
    }
    val drawer = DrawerBuilder()
            .withActivity(activity)
            .withToolbar(toolbar)
            .withActionBarDrawerToggle(true)
            .withActionBarDrawerToggleAnimated(true)
            .withCloseOnClick(true)
            .withSelectedItem(itemIdentifier)
            .addDrawerItems(
                    drawerEmptyItem,
                    drawerItemSettings,
                    drawerItemEventDisplay,
                    drawerItemCommands,
                    DividerDrawerItem(),
                    drawerNotificationSettings,
                    drawerApplicationSettings,
                    drawerGrafana,
                    drawerPrivacyPolicy,
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                    view!!
                    when(drawerItem.identifier){
                        1L -> launchActivity<SettingsActivity>(view, activity, onActivityIntentRequest)
                        2L -> launchActivity<EventDisplayActivity>(view, activity, onActivityIntentRequest)
                        3L -> launchActivity<CommandActivity>(view, activity, onActivityIntentRequest)
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
                        102L -> {
                            val profile = createConnectionProfileManager(activity).activeProfile.profile
                            val couchDb = profile.databaseConnectionProfile as CouchDbDatabaseConnectionProfile
                            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://${couchDb.host}/grafana")))
                        }
                        103L -> {
                            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wildmountainfarms/solarthing-android/blob/master/privacy_policy.md")))
                        }
                    }
                    return true
                }
            })
            .build()
    return DrawerHandler(itemIdentifier, drawer)
}
private inline fun <reified T> launchActivity(view: View, currentActivity: Activity, onActivityIntentRequest: (View, Intent) -> Unit) {
    if(currentActivity is T){
        return
    }
    val intent = Intent(currentActivity, T::class.java)
    onActivityIntentRequest(view, intent)
}
