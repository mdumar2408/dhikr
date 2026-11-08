// settings.gradle.kts — the first file Gradle reads.
// It declares WHERE plugins and libraries are downloaded from, and WHICH modules make up the build.

pluginManagement {
    repositories {
        google {
            // Only look in Google's Maven repo for Android/Google artifacts (faster, safer resolution).
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Modules may not declare their own repositories; everything is centralised here.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Dhikr"
include(":app")
