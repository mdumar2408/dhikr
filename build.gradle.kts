// Root build file. Plugins are declared here with `apply false` so every module
// uses the same plugin versions, but nothing is applied to the root project itself.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 has built-in Kotlin support, so the old `org.jetbrains.kotlin.android`
    // plugin is not needed. The Compose compiler plugin is still required and its
    // version must match the Kotlin version.
    alias(libs.plugins.kotlin.compose) apply false
}
