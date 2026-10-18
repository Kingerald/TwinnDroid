# APK engine libraries use reflection / service lookups internally; keep them whole.
-keep class com.reandroid.** { *; }
-keep class com.android.apksig.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn com.reandroid.**
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**

# Shizuku user service is instantiated by class name in another process.
-keep class com.twinndroid.install.ShellService { *; }
-keep class com.twinndroid.install.IShellService** { *; }
-keep class rikka.shizuku.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.twinndroid.** {
    *** Companion;
    *** serializer(...);
    <fields>;
}
-keep,includedescriptorclasses class com.twinndroid.**$$serializer { *; }

# Launcher aliases reference MainActivity by name.
-keep class com.twinndroid.MainActivity
