# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NativeBridge
-keep class com.controllers.app.NativeBridge { *; }
