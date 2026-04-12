import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
    alias(libs.plugins.firebase.crashlytics.plugin)
    jacoco
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "com.xaymaca.sit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xaymaca.sit"
        minSdk = 26
        targetSdk = 35
        // Allow GitHub Actions to override version code and name from the tag/run number
        versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: 27
        versionName = project.findProperty("versionName")?.toString() ?: "1.4.12"
        resourceConfigurations += listOf("en", "es", "fr", "de", "it", "nl", "el", "pl", "ro", "hu", "pt", "sv", "cs")
    }

    signingConfigs {
        create("release") {
            // Prioritize environment variables for GitHub Actions/CI
            val keystoreFile = System.getenv("RELEASE_STORE_FILE") ?: localProperties.getProperty("RELEASE_STORE_FILE")
            storeFile = keystoreFile?.let { file(it) }
            storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: localProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: localProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: localProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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

// Play Publisher configuration for Google Play Store deployment via GitHub Actions
play {
    // serviceAccountCredentials can be a file or a JSON string from environment variable
    val serviceAccountFile = file("play-service-account.json")
    if (serviceAccountFile.exists()) {
        serviceAccountCredentials.set(serviceAccountFile)
    } else {
        // Fallback to environment variable if file is missing (e.g. on CI)
        val jsonKey = System.getenv("PLAY_SERVICE_ACCOUNT_JSON")
        if (!jsonKey.isNullOrEmpty()) {
            serviceAccountCredentials.set(file("play-service-account-ci.json").apply {
                writeText(jsonKey)
            })
        }
    }
    
    // Support dynamic track selection via command line property -PplayTrack
    track.set(project.findProperty("playTrack")?.toString() ?: "internal")
    defaultToAppBundles.set(true)
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

    // Firebase (crash reporting only — Analytics intentionally excluded)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    debugImplementation(libs.compose.ui.tooling)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.gson)
    testImplementation(libs.kotlinx.coroutines.test)
}

// ---------------------------------------------------------------------------
// Screenshot prep task — run before taking Play Store screenshots
// Usage: ./gradlew screenshotPrep
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

        // Show full signal bars + Wi-Fi
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

// ---------------------------------------------------------------------------
// Release tagging — auto-creates an annotated git tag after a signed release
// build.
// ---------------------------------------------------------------------------
tasks.register("tagRelease") {
    group = "release"
    description = "Creates and pushes an annotated git tag for the current release build"

    mustRunAfter("assembleRelease", "bundleRelease")

    doLast {
        val versionName = android.defaultConfig.versionName
        val versionCode = android.defaultConfig.versionCode
        val tagName = "android/v$versionName-$versionCode"
        val message = "Android release $versionName (versionCode $versionCode)"

        fun git(vararg args: String): String {
            val process = ProcessBuilder(listOf("git") + args.toList())
                .directory(rootProject.projectDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            return output
        }

        // Warn if working tree is dirty but don't block
        val status = git("status", "--porcelain")
        if (status.isNotEmpty()) {
            println("⚠️  Working tree has uncommitted changes — tagging anyway.")
        }

        // Check if tag already exists
        val existing = git("tag", "-l", tagName)
        if (existing == tagName) {
            println("⚠️  Tag $tagName already exists — skipping.")
            return@doLast
        }

        git("tag", "-a", tagName, "-m", message)
        git("push", "origin", tagName)

        println("")
        println("🏷️  Tagged and pushed: $tagName")
        println("    $message")
    }
}
