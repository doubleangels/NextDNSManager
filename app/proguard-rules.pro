-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,Synthetic,EnclosingMethod
-keepattributes Annotation

# Keep entry points in AndroidManifest.xml
-keep class com.doubleangels.nextdnsmanagement.** { *; }
-keep class androidx.** { *; }
-keep class io.sentry.** { *; }
-keep class org.mozilla.** { *; }

# Keep models for Retrofit
-keep class com.squareup.retrofit2.** { *; }

# Keep models and interfaces for ProGuard to work with Reflection
-keep,allowobfuscation @interface *

# Keep classes that are used for serialization/deserialization with Gson
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep entry points for ViewModel and LiveData
-keepclassmembers,allowobfuscation class * {
    @androidx.lifecycle.ViewModelProvider$NewInstanceFactory <init>(...);
}


# Keep Kotlin metadata for Kotlin reflection
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# GeckoView specific rules
-keep class org.mozilla.geckoview.** { *; }

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
