-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

-dontwarn androidx.media3.**

-keep class * extends androidx.room3.RoomDatabase
-keep @androidx.room3.Entity class *
-dontwarn androidx.room3.paging.**

-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

-keep class com.frameout.codecpack.FfmpegBridge { *; }
-keepclassmembers class com.frameout.codecpack.FfmpegBridge {
    native <methods>;
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn kotlinx.serialization.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
}

-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.odml.image.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.firebase.components.** { *; }
-dontwarn com.google.mlkit.**
