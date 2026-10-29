# LiveKit
-keep class io.livekit.** { *; }
-dontwarn io.livekit.**

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# SQLCipher
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**
-keepclasseswithmembernames class * {
    @javax.inject.Inject <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.serialization.** { *; }
-keep class com.mutepoint.app.** { @kotlinx.serialization.Serializable *; }
-dontwarn kotlinx.serialization.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# SQLCipher (additional)
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin intrinsics (required by R8-minified test instrumentation)
-keep class kotlin.jvm.internal.** { *; }
-dontwarn kotlin.jvm.internal.**

# Error-prone annotations (optional, used by androidx.test)
-dontwarn com.google.errorprone.annotations.**
