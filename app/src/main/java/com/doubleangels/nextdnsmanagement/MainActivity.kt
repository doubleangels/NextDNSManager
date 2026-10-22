package com.doubleangels.nextdnsmanagement

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.doubleangels.nextdnsmanagement.biometriclock.BiometricLock
import com.doubleangels.nextdnsmanagement.data.AppPreferences
import com.doubleangels.nextdnsmanagement.ui.NextDnsApp
import com.doubleangels.nextdnsmanagement.ui.SessionViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionViewModel

    /** True while app lock hides the content and waits for authentication. */
    private var locked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        session = ViewModelProvider(this)[SessionViewModel::class.java]
        splash.setKeepOnScreenCondition { !session.ready }
        enableEdgeToEdge()
        setContent {
            NextDnsApp(
                session = session,
                locked = locked,
                onUnlock = ::unlock,
                onReady = ::evaluateLock,
                onSignedIn = { FlavorHooks.onSignedIn(this) },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        evaluateLock()
    }

    /** Locks the app when app lock is on and the last unlock is older than [AUTH_TIMEOUT_MS]. */
    private fun evaluateLock() {
        if (!AppPreferences.isReady) return
        val enabled = AppPreferences.appLock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Keep the dashboard out of the Recents thumbnail while app lock is on.
            setRecentsScreenshotEnabled(!enabled)
        }
        if (!enabled) {
            locked = false
            return
        }
        val elapsed = System.currentTimeMillis() - AppPreferences.lastAuthenticated
        // A negative value means the clock was moved back; never treat that as recent.
        if (elapsed < 0 || elapsed > AUTH_TIMEOUT_MS) locked = true
    }

    private fun unlock() {
        val lock = BiometricLock(this)
        if (!lock.canAuthenticate()) return
        lock.showPrompt(
            getString(R.string.unlock_title),
            getString(R.string.unlock_description),
            "",
            object : BiometricLock.BiometricLockCallback {
                override fun onAuthenticationSucceeded() {
                    AppPreferences.lastAuthenticated = System.currentTimeMillis()
                    locked = false
                }

                override fun onAuthenticationError(errorCode: Int, error: String?) = Unit
                override fun onAuthenticationFailed() = Unit
            },
        )
    }

    private companion object {
        const val AUTH_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
