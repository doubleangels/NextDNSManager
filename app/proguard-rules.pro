# Add project specific ProGuard rules here.
# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# Gson 2.2.4 specific rules
-keepattributes Signature, *Annotation*, EnclosingMethod
-keep class com.google.gson.stream.** { *; }

# Retrofit 2.X
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions

# Keep Retrofit annotations and their methods
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit interface annotations
-keep,allowobfuscation interface retrofit2.Callback

# Keep Retrofit method parameters (e.g., @Path, @Query, @Body)
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}

# Keep Lifecycle ViewModel classes
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
