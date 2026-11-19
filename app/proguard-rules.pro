-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
