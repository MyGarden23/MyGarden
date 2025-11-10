# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== CRITICAL: Kotlin and Firebase Serialization =====

# Kotlin Metadata - Required for reflection
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ===== Firebase Firestore Serialization =====

# Keep all model classes and their no-arg constructors (Firebase needs these)
-keep class com.android.mygarden.model.** { *; }
-keepclassmembers class com.android.mygarden.model.** {
    <init>();
}

# Specifically keep Serialized classes (these are what Firestore actually uses)
-keep class com.android.mygarden.model.plant.SerializedPlant { *; }
-keep class com.android.mygarden.model.plant.SerializedOwnedPlant { *; }
-keep class com.android.mygarden.model.profile.Profile { *; }

# Keep all enum classes (like PlantHealthStatus, GardeningSkill)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Keep Firestore annotations
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
    @com.google.firebase.firestore.ServerTimestamp <methods>;
}

# Keep all Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep all Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Kotlinx Serialization
-keepattributes InnerClasses
-dontwarn kotlinx.serialization.KSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.android.mygarden.**$$serializer { *; }
-keepclassmembers class com.android.mygarden.** {
    *** Companion;
}
-keepclasseswithmembers class com.android.mygarden.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== Other Android/Kotlin Essentials =====

# Keep Kotlin data classes constructors
-keepclassmembers class **.*$Companion { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep coroutines
-keep class kotlin.coroutines.Continuation

# Keep R8 from removing default parameter constructors
-keepclassmembers class * {
    public <init>(...);
}