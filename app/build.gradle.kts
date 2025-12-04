plugins {
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.sonar)
    id("jacoco")
    alias(libs.plugins.compose.compiler)
}
jacoco {
    toolVersion = "0.8.11"
}

android {
    namespace = "com.android.mygarden"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.mygarden"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Enable multidex for large applications
        multiDexEnabled = true

        // Load API key from local.properties
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }

        // Make the API key available in BuildConfig
        val plantnetApiKey =
            properties.getProperty("PLANTNET_API_KEY") ?: System.getenv("PLANTNET_API_KEY") ?: ""
        buildConfigField("String", "PLANTNET_API_KEY", "\"$plantnetApiKey\"")
    }

    // Signing configuration for release builds
    signingConfigs {
        create("release") {
            // These will be provided by GitHub Actions environment variables
            storeFile = file("../release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use the signing config for release builds
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testCoverage {
        jacocoVersion = "0.8.11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        // Add arguments for instrumentation tests
        animationsDisabled = true
    }

    // Robolectric needs to be run only in debug. But its tests are placed in the shared source set (test)
    // The next lines transfers the src/test/* from shared to the testDebug one
    //
    // This prevent errors from occurring during unit tests
    sourceSets.getByName("testDebug") {
        val test = sourceSets.getByName("test")

        java.setSrcDirs(test.java.srcDirs)
        res.setSrcDirs(test.res.srcDirs)
        resources.setSrcDirs(test.resources.srcDirs)
    }

    sourceSets.getByName("test") {
        java.setSrcDirs(emptyList<File>())
        res.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
}

sonar {
    properties {
        property("sonar.projectKey", "MyGarden23_MyGarden")
        property("sonar.projectName", "MyGarden")
        property("sonar.organization", "mygarden23")
        property("sonar.host.url", "https://sonarcloud.io")

        // Silence: "Default to 'debug'"
        property("sonar.androidVariant", "debug")

        // ---- report paths = STRINGS ----
        property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest")
        property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")

        // ---- dirs = MUTABLE LISTS (not String) ----
        property("sonar.sources", mutableListOf("src/main/java"))
        property("sonar.tests", mutableListOf("src/test/java", "src/androidTest/java"))

        // Optional exclusions (string)
        property("sonar.exclusions", "**/R.class,**/R$*.class,**/BuildConfig.*,**/Manifest*.*,android/**/*.*, **/ui/theme/**")
    }
}

// When a library is used both by robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
    androidTestImplementation(dep)
    testImplementation(dep)
}

dependencies {
    // ----- BOMs -----
    val composeBom = platform(libs.compose.bom)

    // =======================
    // Main implementation
    // =======================
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.preview)

    // Multidex support
    implementation(libs.androidx.multidex)

    // Firebase (Auth, DB, Firestore, Storage, Messaging)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    // Firebase AI Logic (Gemini/Imagen)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)

    // Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // Coil
    implementation(libs.coil.compose)

    // Mockito
    implementation(libs.mockito.core)
    implementation(libs.mockito.kotlin)

    // Coroutines Play Services
    implementation(libs.kotlinx.coroutines.play.services)

    // EXIF
    implementation(libs.androidx.exifinterface)

    // =======================
    // Debug-only deps
    // =======================
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)

    // =======================
    // Unit tests (JVM)
    // =======================
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)

    // shared deps test + androidTest
    globalTestImplementation(libs.androidx.junit)
    globalTestImplementation(libs.androidx.espresso.core)
    globalTestImplementation(libs.androidx.espresso.intents)
    globalTestImplementation(composeBom)
    globalTestImplementation(libs.compose.test.junit)
    globalTestImplementation(libs.kaspresso)
    globalTestImplementation(libs.kaspresso.compose)
    globalTestImplementation(libs.turbine)

    // =======================
    // Instrumentation tests (androidTest)
    // =======================
    androidTestImplementation(libs.mockk.android) {
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
        exclude(group = "org.junit") // safe extra guard
    }
    androidTestImplementation(libs.json)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)
}



tasks.withType<Test> {
    // Configure Jacoco for each tests
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
    jvmArgs(
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
    )
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    group = "reporting"
    description = "Generate Jacoco coverage reports."

    reports {
        xml.required = true
        html.required = true
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*",
        "**/Manifest*.*", "**/*Test*.*", "android/**/*.*"
    )

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.layout.projectDirectory}/src/main/java"
    val mainKtSrc = "${project.layout.projectDirectory}/src/main/kotlin"
    sourceDirectories.setFrom(files(mainSrc, mainKtSrc))
    classDirectories.setFrom(files(debugTree))

    // Collect exec data from unit + (optional) connected tests
    val execFiles = fileTree(project.layout.buildDirectory.get()) {
        include(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            "outputs/code_coverage/debugAndroidTest/connected/**/coverage.ec"
        )
    }
    executionData.setFrom(execFiles)

    // If there's no data, skip the task (don't fail the build)
    onlyIf {
        val hasData = execFiles.files.any { it.exists() && it.length() > 0 }
        if (!hasData) {
            logger.lifecycle("No execution data found — skipping jacocoTestReport.")
        }
        hasData
    }

    doLast {
        // New block to modify the XML report after it's generated
        val reportFile = reports.xml.outputLocation.asFile.get()
        if (reportFile.exists()) {
            val content = reportFile.readText()
            val cleanedContent = content.replace("<line[^>]+nr=\"65535\"[^>]*>".toRegex(), "")
            reportFile.writeText(cleanedContent)
        }
    }

    // Ensure unit tests ran
    dependsOn("testDebugUnitTest")
    // do NOT depend on connected tests here; CI runs them separately
}

configurations.forEach { configuration ->
    configuration.exclude("com.google.protobuf", "protobuf-lite")
}
