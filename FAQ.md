# Frequently Asked Questions

Please read this list completely **before** you open an issue or your issue may be closed.

## Are you associated with NextDNS? Is this an official app?

No, this is **not** an official app and I have no ties at all to NextDNS. Unfortunately, this means that the addition of certain features won't be possible. Read more about this below.

## What happened to the official NextDNS app?

The official app appears to have been taken down. On Google Play, there are a number of reasons why an app may be removed or "unlisted", including violation of Google Play policies, requests from the developers themselves, or other reasons. Since I'm not affiliated with the developers, I'm not sure why this has happened or if/when the official app will return.

## What features won't be able to be added to this app since it's not official?

Unless NextDNS/Android make major changes, there are a few features that won't be able to be added. These include:

- Toggle on/off of NextDNS protection, through quick toggles and other means. This is a limitation of Android.
- Connecting through a VPN to NextDNS as was available in the official app. Since I don't have access to official infastructure, there is no VPN server available to facilitate a connection to.
- Changes (additions, removals, edits) to block lists or any of the parental control features.
- Addition of [NXEnhanced](https://github.com/hjk789/NXEnhanced)-like features. The developer of NXEnhanced has ceased development of his project after attempting to work with NextDNS and recieving no response.
- Changes (additions, removals, edits) of NextDNS payment methods.

## Is this app secure? Can you access my account or view my DNS queries?

This is one of the benefits of open source! Anyone can look at all the code and verify for themselves that nothing nafarious is occuring with your data. NextDNS Manager has no access to your account and simply is a way to access the official dashboard on the go. You can think of the app as a very simplified web browser within an app that will only display NextDNS related sites. No information about the app (or your account) leaves your device.

# In the settings for the app, I see a section about Sentry error tracking. Does this mean you're tracking me?

No. [Sentry](https://sentry.io) is a service for developers that gathers information about app crashes, bugs, and other errors and provides them to the developer. This information may contain information about your device (phone type, Android version, etc), about the app (app version, where in the app bugs are occurring, etc), and about the bugs themselves (crash data, stack traces, exceptions, etc). No personal information is collected about you, and nobody other than the maintainer of this project has access to the Sentry error data collected. Furthermore, this is an entirely opt-in option. As of version 5.0.0, there is a toggle in the settings to enable/disable Sentry within your app, and domains to whitelist/blacklist Sentry in your NextDNS configuration are provided. If you choose to disable Sentry, it is not initialized at all. If you choose to enable Sentry, thank you! Your bug and error data helps me push out bug fixes and improvements faster and more reliably.

## How else does this app help protect my privacy? 

In addition to not collecting your data, since version 5.0.0, the app is built around GeckoView. Gecko is the engine that powers Mozilla Firefox, a browser known for protecting privacy and security online.

## I looked in the releases, and the release APK files are large. Why is this? Can you make it smaller?

Since version 5.0.0, the app uses GeckoView from Mozilla. Prior to 5.0.0, the app used Android's WebView, which is already bundled with Android. Since the new GeckoView code is not already bundled with Android like the WebView code is, the APK files are much larger. I will continue to try to lower this size as much as possible, but if download/data usage is important to you, use the Google Play version, as downloads are customized for your device and will be smaller.

## I am new to using NextDNS and I don't understand how to use it, can you teach me?

Sorry, but no. There's tons of excellent documentation out there as to how to effectively use NextDNS, and an amazing community who can help you as well. I simply don't have the time to do tech support.

## I've read all of this and still have a question, what now?

Please open an [issue](https://github.com/doubleangels/NextDNSManager/issues) or [ask the community](https://github.com/doubleangels/NextDNSManager/discussions/categories/q-a).