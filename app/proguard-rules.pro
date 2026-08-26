# Keep Compose metadata used by tooling and reflection-free generated code.
-keepattributes *Annotation*

# Backdrop is a Compose-only rendering library. Its public API is referenced
# directly, but keeping the package makes its runtime-shader behavior
# deterministic across R8 updates.
-keep class com.kyant.backdrop.** { *; }
-dontwarn org.jetbrains.annotations.**
