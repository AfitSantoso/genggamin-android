# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Custom Rules for Genggamin Mobile ---

# Keep data classes used for JSON serialization (Retrofit/Gson)
-keep class com.example.genggaminmobile.data.remote.response.** { *; }
-keep class com.example.genggaminmobile.data.remote.request.** { *; }
-keep class com.example.genggaminmobile.data.model.** { *; }

# Keep Room entities
-keep class com.example.genggaminmobile.data.local.entity.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-dontwarn com.google.gson.reflect.UnsafeReflectionAccessor
-keep class com.google.gson.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.annotation.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.Provides *;
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Room
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}

# Firebase
-keepattributes *Annotation*
-keepclassmembers class com.google.firebase.** {
  *;
}

# Coil
-keep class coil.** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }

# General
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn okio.**