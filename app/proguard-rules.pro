# Keep Compose metadata used by tooling and reflection-free generated code.
-keepattributes *Annotation*

# Backdrop is a Compose-only rendering library. Its public API is referenced
# directly, but keeping the package makes release behavior deterministic across
# R8 updates while the project is still in its visual-prototype phase.
-keep class com.kyant.backdrop.** { *; }
-dontwarn org.jetbrains.annotations.**
