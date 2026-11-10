import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun cfg(name: String, fallback: String): String =
    (findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: fallback

// Google's public AdMob TEST ids (safe default).
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testAppOpenId = "ca-app-pub-3940256099942544/9257395921"

val keystorePath: String? = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() && file(it).exists() }

android {
    namespace = "com.fws.smarttrace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fws.smarttrace"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField(
            "String", "PRIVACY_URL",
            "\"" + cfg("PRIVACY_URL", "https://YOUR_GITHUB_USERNAME.github.io/SmartTrace/privacy-policy.html") + "\""
        )
        buildConfigField("String", "SUPPORT_EMAIL", "\"17eathday@gmail.com\"")

        // Optional: smaller sideload APKs, e.g. -PABI_FILTERS=arm64-v8a,armeabi-v7a (CI debug build uses this).
        // Release/AAB builds do not pass it, so Google Play still gets every ABI.
        (findProperty("ABI_FILTERS") as String?)?.takeIf { it.isNotBlank() }?.let { abis ->
            ndk { abiFilters += abis.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
        }
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$testAppOpenId\"")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = cfg("ADMOB_APP_ID", testAppId)
            buildConfigField("String", "ADMOB_BANNER_ID", "\"" + cfg("ADMOB_BANNER_ID", testBannerId) + "\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"" + cfg("ADMOB_APP_OPEN_ID", testAppOpenId) + "\"")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Keep every language inside the AAB so the in-app language switch works on all devices.
    bundle {
        language { enableSplit = false }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += "UseKtx" // style-only suggestions
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // On-device ML (bundled models: work fully offline)
    implementation("com.google.android.gms:play-services-tasks:18.2.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Google Play: In-App Update (Flexible), AdMob + consent (UMP)
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
}
