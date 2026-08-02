# ProGuard rules for RavenHub Release build

# Keep All RavenHub App Classes & Components
-keep class com.ravenhub.app.** { *; }
-keepclassmembers class com.ravenhub.app.** { *; }

# WorkManager, Room Database & AndroidX Startup
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }
-dontwarn androidx.work.**

-keep class androidx.room.** { *; }
-keepclassmembers class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

-keep class androidx.startup.** { *; }
-keepclassmembers class androidx.startup.** { *; }
-dontwarn androidx.startup.**

# Dev Chrisbanes Haze (Expressive Blur)
-keep class dev.chrisbanes.haze.** { *; }
-keepclassmembers class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# Biometric API
-keep class androidx.biometric.** { *; }
-keepclassmembers class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# UniFFI / Rust Security Bridge & JNA
-keep class uniffi.raven_security.** { *; }
-keepclassmembers class uniffi.raven_security.** { *; }
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure { *; }
-dontwarn com.sun.jna.**
-dontwarn uniffi.raven_security.**

# libsu / Root Shell
-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**

# UCrop & Coil Image Loader
-keep class com.yalantis.ucrop.** { *; }
-keepclassmembers class com.yalantis.ucrop.** { *; }
-dontwarn com.yalantis.ucrop.**
-keep class coil3.** { *; }
-keep class coil.** { *; }
-dontwarn coil3.**
-dontwarn coil.**

# kotlinx.serialization & Data Models
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class com.ravenhub.app.data.** { *; }
-keepclassmembers class com.ravenhub.app.data.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * extends java.lang.Enum { *; }

# Android & LSPosed / RavenHub Engine
-keep class ravenhub.engine.AppMonitor {
    public static void main(java.lang.String[]);
}

-keep class ravenhub.engine.SysMonMain {
    public static void main(java.lang.String[]);
}

-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn org.lsposed.hiddenapibypass.**