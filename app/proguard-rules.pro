# ProGuard rules
-keepattributes *Annotation*
-keep class com.drink.watertracker.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.glance.**
