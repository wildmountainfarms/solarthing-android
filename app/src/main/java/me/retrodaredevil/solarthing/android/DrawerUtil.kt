package me.retrodaredevil.solarthing.android

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem

/*
Thanks https://android.jlelse.eu/android-using-navigation-drawer-across-multiple-activities-the-easiest-way-b011f152aebd
and of course thanks https://github.com/mikepenz/MaterialDrawer
 */

fun getDrawer(activity: Activity, toolbar: Toolbar) {
    //if you want to update the items at a later time it is recommended to keep it in a variable
    val drawerEmptyItem = PrimaryDrawerItem().withIdentifier(0).withName("")
    drawerEmptyItem.withEnabled(false)

    val drawerItemSettings: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(1)
        .withName(R.string.settings_select)
    val drawerItemEventDisplay: PrimaryDrawerItem = PrimaryDrawerItem().withIdentifier(2)
        .withName(R.string.event_display_select)//.withIcon(R.drawable.tournamenticon)


//    val drawerItemAbout: SecondaryDrawerItem = SecondaryDrawerItem().withIdentifier(4)
//        .withName(R.string.about).withIcon(R.drawable.ic_info_black_24px)

    //create the drawer and remember the `Drawer` result object
    val result = DrawerBuilder()
        .withActivity(activity)
        .withToolbar(toolbar)
        .withActionBarDrawerToggle(true)
        .withActionBarDrawerToggleAnimated(true)
        .withCloseOnClick(true)
        .withSelectedItem(-1)
        .addDrawerItems(
            drawerEmptyItem, drawerEmptyItem, drawerEmptyItem,
            drawerItemSettings,
            drawerItemEventDisplay
//            DividerDrawerItem(),
        )
        .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
            override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                view!!
                if (drawerItem.identifier == 2L && activity !is SettingsActivity) {
                    // load tournament screen
                    val intent = Intent(activity, SettingsActivity::class.java)
                    view.context.startActivity(intent)
                }
                return true
            }
        })
        .build()
}
