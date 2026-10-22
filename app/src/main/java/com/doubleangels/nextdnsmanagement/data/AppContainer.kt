package com.doubleangels.nextdnsmanagement.data

import com.doubleangels.nextdnsmanagement.api.NextDnsApi
import com.doubleangels.nextdnsmanagement.api.NextDnsRepository

/** Process-wide singletons. The API reads the key on every request, so it follows sign-in/out. */
object AppContainer {
    val repository: NextDnsRepository by lazy { NextDnsRepository(NextDnsApi { AppPreferences.apiKey }) }
}
