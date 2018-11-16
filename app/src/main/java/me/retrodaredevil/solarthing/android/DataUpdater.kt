package me.retrodaredevil.solarthing.android

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.SystemClock
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler

class DataUpdater(
    private val context: Context,
    private val notificationHandler: NotificationHandler
) {

    private val handler = Handler()

    fun start(){
        run()
    }
    private fun run() {
        DataUpdaterTask {notificationHandler.updateStatusNotification(context)}.execute()
        handler.postAtTime(this::run, SystemClock.uptimeMillis() + 1000 * 60)
    }

}
private class DataUpdaterTask(private val updateNotification: () -> Unit) : AsyncTask<Void, Void, Boolean>() {
    override fun doInBackground(vararg params: Void?): Boolean {
        return RecentData.updateData()
    }

    override fun onPostExecute(result: Boolean?) {
        updateNotification()
    }

}
