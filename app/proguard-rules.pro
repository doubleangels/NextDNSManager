# Keep all classes in the support libraries
-keep class androidx.appcompat.** { *; }
-keep class androidx.preference.** { *; }
-keep class com.google.android.material.** { *; }

# Keep all classes in other dependencies
-keep class com.jakewharton.processphoenix.** { *; }
-keep class com.squareup.retrofit2.** { *; }
-keep class io.sentry.** { *; }
-keep class org.mozilla.geckoview.** { *; }

# Keep all public and protected methods in the above libraries
-keepclassmembers class androidx.appcompat.** { public protected *; }
-keepclassmembers class androidx.preference.** { public protected *; }
-keepclassmembers class com.google.android.material.** { public protected *; }
-keepclassmembers class com.jakewharton.processphoenix.** { public protected *; }
-keepclassmembers class com.squareup.retrofit2.** { public protected *; }
-keepclassmembers class io.sentry.** { public protected *; }
-keepclassmembers class org.mozilla.geckoview.** { public protected *; }

# Keep the Retrofit interfaces and their methods
-keep interface retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep the Gson classes
-keep class com.google.gson.** { *; }
-keep class org.apache.commons.** { *; }

# Keep enum types and fields
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep the entry point for apps
-keepattributes *Annotation*
-keepclassmembers class * {
    public static void main(java.lang.String[]);
}

# This is generated automatically by the Android Gradle plugin.
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor