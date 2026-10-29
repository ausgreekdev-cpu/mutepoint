# LiveKit
-keep class io.livekit.** { *; }
-dontwarn io.livekit.**

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# SQLCipher
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# Error-prone annotations (optional, used by androidx.test)
-dontwarn com.google.errorprone.annotations.**
