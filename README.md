# SolarThing Android
The android application to display solarthing data as a notification

NOTE: More up to date documentation can be found here: https://github.com/wildmountainfarms/solarthing

SolarThing can be used to monitor systems with FX inverters and MX or Rover Charge Controllers.

---

## Using
Available at https://play.google.com/store/apps/details?id=me.retrodaredevil.solarthing.android

For use with https://github.com/wildmountainfarms/solarthing

This requires Android 8+.

## Features
* Adds a persistent notification that is updated constantly
* See the battery voltage, load, pv power, daily kWh
* Separate notification when generator is running
* Daily notification for connected devices that informs you of previous day's statistics

Known Bugs:
* If you have multiple profiles, sometimes one will randomly overwrite the other.
* When editing a profile when "Auto Network Switching Enable" is enabled, there might be bugs when saving (need to look into this further)
* When pressing save at the same time the PersistentService changes the activeUUID, one may overwrite the other
  * This is highly unlikely

### Features to Add:
* Notification when the generator turns off telling you some useful information
* Add more widgets
* If a solar packet just has FX's, remove some unnecessary information (completed for MX's and Rover)
* Clearable notifications for errors
* Multiple configurable voltage timers
* OTG Cable Outback MATE Serial display
* Use Ektorp's ChangeFeed class to make sure we get ALL of the packets since the last time we queried
  * ~Don't request all events each time~
* Add a notification for when the connection has failed for a long time
* Add a notification for when the data is wayy out of date
* Add "turbo" mode for reloading data
* https://bitbucket.org/StringMon/unofficialtileapi/src/master/
  * For tiles
* Lock "more" notifications
* Only switch networks if previous network failed option

### Features to Add that I am unable to test:
* Supporting systems that don't have both FX's and MX's
* FlexNET DC packets (this still has to be implemented on the main solarthing codebase anyway)
* Implementing a "sell" status. Some systems are able to give power back to the grid, it might be useful to have
information that dynamically appears if we are selling
* On the current system I test with, amp hours is not supported, this would be a useful feature to have if the system supports it.
* I am unable to test 230V FX setups. I believe the implementation is correct, but it is mostly untested

### To do
* Add option to rename generator to something like "Power Grid" for systems that use
the power grid as backup power instead of a generator that can be turned on and off
* Modularize parts of the program different modules (ex: solar, outhouse, etc) can be enabled or disabled easily
* Make celsius and fahrenheit use special degrees character
* https://developer.android.com/studio/build/shrink-code#decode-stack-trace

### Google Play Requirements
Since SolarThing uses the location permission, this YouTube video shows how it is used: https://youtu.be/u9-quz89KQw

### Contributing
Contributions are welcome. Right now this app is set up for my specific use case:
It has a persistent notification for solar data.

If you want to set this up for a use case specific to you, 
you're welcome to fork this and make as many changes as you want.
You can, however, submit suggestions on how to customize this application to fit
as many use cases as possible. I recommend opening an issue and getting some thoughts on it.

There are a few things I am unable to test because I have only tested with a system with both FX's and MX's.
Some of these features are in the "Features to Add that I am unable to test" and "Future changes to make"

### Code Style
* Use continuation indent for everything Android gives you the option

### Developer Notes

* Upgrade to SDK Version 33 - things that changed
  * "Permission for notifications"
    * We need the "POST_NOTIFICATIONS" permission and must explicitly request permission to send notifications
    * We should eventually follow best practices: https://developer.android.com/develop/ui/views/notifications/notification-permission#best-practices
      * [trigger permission prompt](https://developer.android.com/training/permissions/requesting#request-permission)
      * [areNotificationsEnabled()](https://developer.android.com/reference/android/app/NotificationManager#areNotificationsEnabled())
  * "Battery resource utilization"
    * We might need to worry about this. We can look at this later
    * https://support.google.com/pixelphone/answer/7015477
    * https://developer.android.com/topic/performance/background-optimization#bg-restrict
