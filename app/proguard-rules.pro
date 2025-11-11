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

# Specifically keep Serialized classes (redundant with above, but kept for documentation)
# These are the core classes that Firestore actually uses - if something breaks, check these first
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

# Keep Parcelable implementations in model classes
-keepclassmembers class com.android.mygarden.model.** implements android.os.Parcelable {
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

# Keep Kotlin companion objects in model classes (needed for Firebase serialization)
-keepclassmembers class com.android.mygarden.model.**$Companion { *; }

# Keep coroutines
-keep class kotlin.coroutines.Continuation

# Keep constructors in model classes (Firebase needs these for deserialization)
-keepclassmembers class com.android.mygarden.model.** {
    public <init>(...);
}

# ===== Android @Keep Annotation Support =====

# Keep classes and members annotated with @Keep
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ===== Navigation Component (if used) =====

# Uncomment if you use Jetpack Navigation with Safe Args
#-keepnames class androidx.navigation.fragment.NavHostFragment
#-keep class * extends androidx.navigation.Navigator