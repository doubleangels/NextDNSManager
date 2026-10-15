# Keep WebView JavaScript bridge methods.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.doubleangels.nextdnsmanagement.webview.WebAppInterface { *; }

-keepattributes *Annotation*
-dontwarn com.doubleangels.nextdnsmanagement.**

# General ProGuard rules
-dontwarn javax.annotation.**

# AndroidX App Startup (InitializationProvider runs before Application.onCreate)
-keep class androidx.startup.** { *; }
-keep class * extends androidx.startup.Initializer {
    <init>();
}

# Webkit
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# OkHttp (VisualIndicator DNS checks)
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson (VisualIndicator JSON parsing)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keep class de.hdodenhof.circleimageview.** { *; }
-dontwarn de.hdodenhof.circleimageview.**

# Sentry Android
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# LeakCanary (debug only, no need to obfuscate for debug builds)
-dontwarn com.squareup.leakcanary.**
-keep class com.squareup.leakcanary.** { *; }
