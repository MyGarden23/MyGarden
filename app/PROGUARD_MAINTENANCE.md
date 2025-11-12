# ProGuard Rules Maintenance Guide

This guide explains how to maintain and update the ProGuard rules in `proguard-rules.pro` to ensure
your app works correctly in release builds.

## Table of Contents

- [What is ProGuard/R8?](#what-is-proguardr8)
- [When to Update ProGuard Rules](#when-to-update-proguard-rules)
- [How to Test ProGuard Rules](#how-to-test-proguard-rules)
- [Common Issues and Solutions](#common-issues-and-solutions)
- [Rule Categories Explained](#rule-categories-explained)
- [Best Practices](#best-practices)

---

## What is ProGuard/R8?

ProGuard/R8 is Android's code shrinker and obfuscator that:

- **Shrinks** code by removing unused classes and methods
- **Obfuscates** code by renaming classes/methods to shorter names
- **Optimizes** bytecode for better performance

However, this can break features that rely on reflection, serialization, or dynamic class loading (
like Firebase Firestore). That's why we need to tell ProGuard what to keep.

---

## When to Update ProGuard Rules

Update your ProGuard rules whenever you:

### 1. **Add New Model Classes**

If you create new data models for Firebase Firestore:

```kotlin
// Example: New model class
data class Garden(
    val id: String = "",
    val name: String = "",
    val plants: List<String> = emptyList()
)
```

**Action:** Add a keep rule for the entire package or specific class:

```proguard
-keep class com.android.mygarden.model.garden.** { *; }
```

### 2. **Add New Enums**

Enums are already covered by the generic enum rule, but verify:

```proguard
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}
```

### 3. **Use New Firebase Annotations**

If you use `@PropertyName`, `@DocumentId`, `@Exclude`, etc., they're already covered:

```proguard
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}
```

### 4. **Add Serialization/Parcelable Classes**

These are covered by generic rules, but ensure new implementations work.

### 5. **Add New Third-Party Libraries**

Check the library's documentation for required ProGuard rules. Usually found in:

- Library's GitHub README
- Library's `consumer-proguard-rules.pro` (auto-included)
- Documentation website

### 6. **Use Reflection or Dynamic Class Loading**

If you access classes/methods dynamically by name, keep them:

```proguard
-keep class com.example.DynamicallyLoadedClass { *; }
```

---

## How to Test ProGuard Rules

### 1. **Build a Release APK**

```bash
./gradlew assembleRelease
```

### 2. **Install and Test on Device**

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 3. **Check Logcat for Errors**

Look for these common errors:

- `ClassNotFoundException`
- `NoSuchMethodException`
- `NoSuchFieldException`
- Firebase deserialization errors: "Could not deserialize object"

```bash
adb logcat | grep -i "exception\|error\|firebase"
```

### 4. **Test Critical Features**

Manually test:

- ✅ Firebase Firestore read/write operations
- ✅ User profile loading
- ✅ Plant data serialization
- ✅ All enum usages
- ✅ Navigation between screens
- ✅ Authentication flows

### 5. **Analyze the Build**

Check what was removed:

```bash
cat app/build/outputs/mapping/release/usage.txt
```

Check what was renamed:

```bash
cat app/build/outputs/mapping/release/mapping.txt
```

---

## Common Issues and Solutions

### Issue: Firebase Firestore Deserialization Fails

**Symptoms:**

```
Could not deserialize object. Class com.android.mygarden.model.plant.SerializedPlant does not define a no-argument constructor.
```

**Solution:**
Add specific keep rules for the model class:

```proguard
-keep class com.android.mygarden.model.plant.SerializedPlant { *; }
-keepclassmembers class com.android.mygarden.model.plant.SerializedPlant {
    <init>();
}
```

### Issue: Enum Values Return Null

**Symptoms:**

```
NullPointerException when accessing enum values
```

**Solution:**
Ensure the enum keep rule is present (already in our config):

```proguard
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}
```

### Issue: Kotlin Reflection Fails

**Symptoms:**

```
KotlinReflectionInternalError
```

**Solution:**
Keep Kotlin metadata (already in our config):

```proguard
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
```

### Issue: Serialization Breaks

**Symptoms:**

- kotlinx.serialization fails
- @Serializable classes don't work

**Solution:**
Keep serializers (already in our config):

```proguard
-keep,includedescriptorclasses class com.android.mygarden.**$$serializer { *; }
-keepclasseswithmembers class com.android.mygarden.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

---

## Rule Categories Explained

### 1. **Kotlin & Metadata**

Keeps Kotlin-specific features working:

```proguard
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*
```

### 2. **Firebase Firestore Models**

Prevents Firebase serialization issues:

```proguard
-keep class com.android.mygarden.model.** { *; }
```

⚠️ **Broad rule** - keeps all model classes. Consider making it more specific if APK size is a
concern.

### 3. **Enums**

Ensures enum functionality (values(), valueOf()):

```proguard
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
```

### 4. **Parcelable**

Keeps Android's Parcelable implementation:

```proguard
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
```

### 5. **Kotlinx Serialization**

Keeps generated serializers:

```proguard
-keep,includedescriptorclasses class com.android.mygarden.**$$serializer { *; }
```

---

## Best Practices

### ✅ **DO:**

1. **Test release builds frequently** - Don't wait until release day
2. **Keep rules specific** - Use wildcards carefully to avoid bloating APK
3. **Document custom rules** - Add comments explaining why each rule exists
4. **Version control mapping files** - Save `mapping.txt` for each release to decode crash reports
5. **Use `@Keep` annotation** - For individual classes/methods:
   ```kotlin
   @Keep
   class ImportantClass { }
   ```
6. **Check library documentation** - Most libraries provide their own ProGuard rules

### ❌ **DON'T:**

1. **Don't use `-dontobfuscate`** - This defeats the purpose of ProGuard
2. **Don't blanket keep everything** - Use specific rules instead of `-keep class * { *; }`
3. **Don't ignore warnings in release builds** - Investigate and fix them
4. **Don't forget to test on real devices** - Emulators may not catch all issues
5. **Don't remove auto-generated rules** - Libraries include their own consumer rules

---

## Checklist for Updates

When modifying ProGuard rules, use this checklist:

- [ ] Added keep rules for new model classes
- [ ] Tested Firebase Firestore read/write operations
- [ ] Verified enum functionality
- [ ] Built and tested release APK on physical device
- [ ] Checked logcat for exceptions
- [ ] Reviewed `usage.txt` to ensure nothing critical was removed
- [ ] Saved `mapping.txt` for this version
- [ ] Tested all critical user flows
- [ ] Verified third-party library integrations
- [ ] Updated this document if new patterns emerge

---

---

## Additional Resources

- [Android ProGuard Documentation](https://developer.android.com/studio/build/shrink-code)
- [R8 Full Mode](https://developer.android.com/studio/build/shrink-code#full-mode)
- [Firebase ProGuard Rules](https://firebase.google.com/docs/android/troubleshooting-faq#proguard)
- [Kotlin ProGuard Rules](https://kotlinlang.org/docs/reference/using-gradle.html#proguard)

---

**Transparency Note**  
This guide was drafted with the assistance of an AI tool and subsequently reviewed and refined by a human author to ensure accuracy and clarity.

