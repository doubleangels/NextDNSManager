# Add project specific ProGuard rules here.
# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# Gson 2.2.4 specific rules
-keepattributes Signature, *Annotation*, EnclosingMethod
-keep class com.google.gson.stream.** { *; }

# Keep Lifecycle ViewModel classes
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

# GeckoView rules
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
