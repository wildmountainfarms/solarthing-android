package me.retrodaredevil.solarthing.android.util

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import me.retrodaredevil.solarthing.android.activity.CommandActivity
import me.retrodaredevil.solarthing.android.activity.EventDisplayActivity
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.activity.SettingsActivity

/*
Thanks https://android.jlelse.eu/android-using-navigation-drawer-across-multiple-activities-the-easiest-way-b011f152aebd
and of course thanks https://github.com/mikepenz/MaterialDrawer
 */

fun initializeDrawer(
    activity: Activity,
    toolbar: Toolbar = activity.findViewById(R.id.toolbar),
    onActivityIntentRequest: (View, Intent) -> Unit = { view, intent -> view.context.startActivity(intent) }
) {
    val drawerEmptyItem = PrimaryDrawerItem().withIdentifier(0).withName("")

    val drawerItemSettings: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(1)
        .withName(R.string.settings_select)
    val drawerItemEventDisplay: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(2)
        .withName(R.string.event_display_select)
    val drawerItemCommands: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(3)
        .withName(R.string.commands_select)



//    val drawerItemAbout: SecondaryDrawerItem = SecondaryDrawerItem().withIdentifier(4)
//        .withName(R.string.about).withIcon(R.drawable.ic_info_black_24px)

    val result = DrawerBuilder()
        .withActivity(activity)
        .withToolbar(toolbar)
        .withActionBarDrawerToggle(true)
        .withActionBarDrawerToggleAnimated(true)
        .withCloseOnClick(true)
        .withSelectedItem(when(activity){
            is SettingsActivity -> 1
            is EventDisplayActivity -> 2
            is CommandActivity -> 3
            else -> -1
        })
        .addDrawerItems(
            drawerEmptyItem,
            drawerItemSettings,
            drawerItemEventDisplay,
            drawerItemCommands
//            DividerDrawerItem(),
        )
        .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
            override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                view!!
                when(drawerItem.identifier){
                    1L -> launchActivity<SettingsActivity>(view, activity, onActivityIntentRequest)
                    2L -> launchActivity<EventDisplayActivity>(view, activity, onActivityIntentRequest)
                    3L -> launchActivity<CommandActivity>(view, activity, onActivityIntentRequest)
                }
                return true
            }
        })
        .build()
}
private inline fun <reified T> launchActivity(view: View, currentActivity: Activity, onActivityIntentRequest: (View, Intent) -> Unit) {
    if(currentActivity is T){
        return
    }
    val intent = Intent(currentActivity, T::class.java)
    onActivityIntentRequest(view, intent)
}
