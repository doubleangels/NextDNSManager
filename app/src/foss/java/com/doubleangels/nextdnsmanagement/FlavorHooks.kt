package com.doubleangels.nextdnsmanagement

import androidx.activity.ComponentActivity

/** foss flavor: no Google services, so nothing to set up. */
object FlavorHooks {
    @Suppress("UNUSED_PARAMETER")
    fun onSignedIn(activity: ComponentActivity) = Unit
}
