// Module build file for the single `app` module.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // Kotlin package of the generated R and BuildConfig classes.
    namespace = "com.dhikr.app"

    // compileSdk: the Android API your code is COMPILED against (which APIs you can call).
    // 37 = Android 17, the newest level AGP 9.5 supports; recent AndroidX libraries require it.
    compileSdk = 37

    defaultConfig {
        // applicationId: the app's permanent identity on a device and on Google Play.
        // It can never change after the first Play release.
        applicationId = "com.dhikr.app"

        // minSdk: the oldest Android version allowed to install the app.
        // 26 = Android 8.0. Nothing phones run 14+, but a lower floor keeps the
        // door open for other devices later at almost no cost.
        minSdk = 26

        // targetSdk: the Android version whose runtime behaviour the app has been
        // tested against. 36 = Android 16, which is what Nothing OS 4.1 runs.
        targetSdk = 36

        versionCode = 1        // integer, must increase with every release build
        versionName = "0.1.0"  // human-readable version

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
