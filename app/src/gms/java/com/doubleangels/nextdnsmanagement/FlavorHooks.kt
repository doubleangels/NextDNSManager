package com.doubleangels.nextdnsmanagement

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.doubleangels.nextdnsmanagement.firebasemessaging.MessagingInitializer

/** gms flavor: Firebase Cloud Messaging for announcements. */
object FlavorHooks {
    fun onSignedIn(activity: ComponentActivity) {
        if (!MessagingInitializer.isInitialized()) {
            // initialize() reports its own failures to Sentry.
            Thread({ MessagingInitializer.initialize(activity.applicationContext) }, "fcm-init").start()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        }
    }
}
