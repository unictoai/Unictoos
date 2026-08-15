# Unictoos v0.2 conservative release rules.
# Keep Android-created entry points and constructor signatures used by the platform.
-keep public class com.unictoai.unictoos.ui.MainActivity { *; }
-keep public class com.unictoai.unictoos.streaming.StreamingForegroundService { *; }
-keep public class com.unictoai.unictoos.StudioViewModel { *; }
-keep public class com.unictoai.unictoos.ui.PreviewSurfaceView { *; }

# RootEncoder may discover codec/source implementations through runtime class names.
-keep class com.pedro.** { *; }

# Preserve callback and annotation metadata used by Android and library interfaces.
-keepattributes Exceptions,InnerClasses,Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault
