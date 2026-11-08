import java.util.Properties

// Module build file for the single `app` module.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

// Room writes the schema JSON for every DB version here. It's committed to git and
// used by migration tests (offline-first-android skill).
room3 {
    schemaDirectory("$projectDir/schemas")
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

    // Release signing (D-046). Secrets live in keystore.properties at the repo root, which is
    // git-ignored, as is the keystore itself. Without that file the release APK is built unsigned.
    val keystoreProps = rootProject.file("keystore.properties")
    if (keystoreProps.exists()) {
        val p = Properties().apply { keystoreProps.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: removes unused code and resources, shortens names, optimises (D-046).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
        // Same as release (R8 on) but signed with the debug key and installed beside the
        // debug app (".staging"), so the shrunk build can be tested without touching real data.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    // Declared explicitly (not only transitively) so the app and coroutines-test use the same version.
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.androidx.datastore)
    implementation(libs.reorderable)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room3.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
