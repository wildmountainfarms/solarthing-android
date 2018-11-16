package me.retrodaredevil.solarthing.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler

class PersistentService(): Service(){
    private val notificationUpdater = DataUpdater(this, NotificationHandler)

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationUpdater.start()
        return super.onStartCommand(intent, flags, startId)
    }

}