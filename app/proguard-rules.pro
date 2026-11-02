# Add project specific ProGuard rules here.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class com.example.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn kotlinx.serialization.**
