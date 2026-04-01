plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    jacoco
}

android {
    namespace = "com.xaymaca.sit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xaymaca.sit"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
    }

    signingConfigs {
        create("release") {
            storeFile = project.findProperty("RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.extensions.configure(JacocoTaskExtension::class) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

// JaCoCo coverage report task
tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        // Hilt/Dagger generated
        "**/*_HiltModules*", "**/*_Factory*", "**/*_MembersInjector*",
        "**/Hilt_*", "**/*Hilt*",
        // Room generated
        "**/*_Impl*",
        // Compose compiler generated
        "**/*\$\$inlined*", "**/*ComposableSingletons*",
    )

    val debugTree = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/classes") {
        exclude(fileFilter)
    }
    val kotlinDebugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(files(debugTree, kotlinDebugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec", "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)
    implementation(libs.activity.compose)

    // Room (local database — equivalent to SwiftData)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt (dependency injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Gson (JSON serialization for Room list fields)
    implementation(libs.gson)

    debugImplementation(libs.compose.ui.tooling)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.gson)
}

// ---------------------------------------------------------------------------
// Screenshot prep task — run before taking Play Store screenshots
// Usage: ./gradlew screenshotPrep
//
// What it does:
//   1. Sets the clock to 9:41 AM (standard store screenshot time)
//   2. Sets battery to 100%, plugged-in appearance
//   3. Enables Do Not Disturb (no notification icons)
//   4. Hides the navigation bar demo mode
//   5. Prints reminder to take screenshots, then tears down demo mode
//
// Requires a running emulator or connected device (adb must be on PATH).
// ---------------------------------------------------------------------------
tasks.register("screenshotPrep") {
    group = "screenshot"
    description = "Prepares emulator status bar for clean Play Store screenshots"

    doLast {
        val adb = "adb"

        fun adb(vararg args: String) {
            runCatching {
                ProcessBuilder(listOf(adb) + args.toList()).inheritIO().start().waitFor()
            }
        }

        println("📸  Entering demo mode for screenshots...")

        // Enable demo mode
        adb("shell", "settings", "put", "global", "sysui_demo_allowed", "1")
        adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
            "-e", "command", "enter")

        // Set clock to 9:41
        adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
            "-e", "command", "clock",
            "-e", "hhmm", "0941")

        // Set battery to 100, hide charging icon
        adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
            "-e", "command", "battery",
            "-e", "level", "100",
            "-e", "plugged", "false")

        // Hide notifications
        adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
            "-e", "command", "notifications",
            "-e", "visible", "false")

        // Show full signal bars + wifi
        adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
            "-e", "command", "network",
            "-e", "mobile", "show",
            "-e", "level", "4",
            "-e", "wifi", "show",
            "-e", "wifiLevel", "4",
            "-e", "nosim", "false")

        println("")
        println("✅  Demo mode active. Status bar shows: 9:41 | full signal | 100% battery | no notifications")
        println("📱  Take your screenshots now.")
        println("")
        println("▶  When done, run:  ./gradlew screenshotTeardown")
    }
}

tasks.register("screenshotTeardown") {
    group = "screenshot"
    description = "Exits demo mode and restores normal status bar"

    doLast {
        runCatching {
            ProcessBuilder(listOf("adb", "shell", "am", "broadcast",
                "-a", "com.android.systemui.demo", "-e", "command", "exit"))
                .inheritIO().start().waitFor()
        }
        println("✅  Demo mode exited. Status bar restored.")
    }
}
