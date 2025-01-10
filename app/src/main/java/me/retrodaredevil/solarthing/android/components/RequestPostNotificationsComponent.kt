package me.retrodaredevil.solarthing.android.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import me.retrodaredevil.solarthing.android.service.getManager

/**
 * Manages requesting the notification permission.
 * To instantiate this class, you should use [RequestPostNotificationsComponent.createAndRegister].
 *
 * If you need more control over the registration of this, consider modifying the class.
 * The constructor and register() are both private to prevent you from accidentally forgetting to register this or registering it twice.
 */
class RequestPostNotificationsComponent private constructor(
        private val activity: FragmentActivity
) : DefaultLifecycleObserver {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private fun register(): RequestPostNotificationsComponent {
        activity.lifecycle.addObserver(this)
        return this
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        requestPermissionLauncher = activity.registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(activity, "Enable notifications by navigating to SolarThing's notification settings.", Toast.LENGTH_LONG).show()
            }
        }
    }


    fun requestNotificationPermission(): Boolean {
        if (activity.getManager().areNotificationsEnabled()) {
            return true
        }

        // You can directly ask for the permission.
        // The registered ActivityResultCallback gets the result of this request.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            )
        }

        return false
    }
    companion object {
        fun createAndRegister(activity: FragmentActivity): RequestPostNotificationsComponent {
            return RequestPostNotificationsComponent(activity).register()
        }
    }
}
