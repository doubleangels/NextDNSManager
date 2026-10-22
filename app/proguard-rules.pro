-keepattributes *Annotation*

# General ProGuard rules
-dontwarn javax.annotation.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Sentry Android
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# LeakCanary (debug only, no need to obfuscate for debug builds)
-dontwarn com.squareup.leakcanary.**
-keep class com.squareup.leakcanary.** { *; }
