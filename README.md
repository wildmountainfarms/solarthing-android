# solarthing-android
The android application to display solarthing data as a notification

NOTE: More up to date documentation can be found here: https://github.com/wildmountainfarms/solarthing

---

## Using
Available at https://play.google.com/store/apps/details?id=me.retrodaredevil.solarthing.android

For use with https://github.com/wildmountainfarms/solarthing

This works best with Android 8+, but will still work down to Android 4.4.

Known Bugs:
* When editing a profile when "Auto Network Switching Enable" is enabled, there might be bugs when saving (need to look into this further)
* When pressing save at the same time the PersistentService changes the activeUUID, one may overwrite the other
  * This is highly unlikely

### Features to Add:
* Notification when the generator turns off telling you some useful information
* Drop down menu for the command to send to the MATE
* A packet that the MATE pi uploads that communicates available commands to display
* Add more widgets
* If a solar packet just has FX's, remove some unnecessary information (completed for MX's and Rover)
  * Also remove load information when there are only charge controller packets
* Clearable notifications for errors
* **Low/High temperature notifications**
* Multiple configurable voltage timers
* OTG Cable Outback MATE Serial display

### Features to Add that I am unable to test:
* Supporting systems that don't have both FX's and MX's
* FlexNET DC packets (this still has to be implemented on the main solarthing codebase anyway)
* Implementing a "sell" status. Some systems are able to give power back to the grid, it might be useful to have
information that dynamically appears if we are selling
* On the current system I test with, amp hours is not supported, this would be a useful feature to have if the system supports it.
* I am unable to test 230V FX setups. I believe the implementation is correct, but it is mostly untested

### Future changes to make
* Add option to rename generator to something like "Power Grid" for systems that use
the power grid as backup power instead of a generator that can be turned on and off
* Modularize parts of the program different modules (ex: solar, outhouse, etc) can be enabled or disabled easily

### Contributing
Contributions are welcome. Right now this app is set up for my specific use case:
It has a persistent notification for solar data.

If you want to set this up for a use case specific to you, 
you're welcome to fork this and make as many changes as you want.
You can, however, submit suggestions on how to customize this application to fit
as many use cases as possible. I recommend opening an issue and getting some thoughts on it.

There are a few things I am unable to test because I have only tested with a system with both FX's and MX's.
Some of these features are in the "Features to Add that I am unable to test" and "Future changes to make"
