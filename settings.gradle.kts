pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Tesseract4Android is published on JitPack only. The cz.adaptech alias path 404s (see adaptech-cz/Tesseract4Android#86),
        // so the dependency uses the canonical com.github.* coordinates.
        maven("https://jitpack.io") {
            content { includeGroup("com.github.adaptech-cz.Tesseract4Android") }
        }
    }
}
rootProject.name = "Frame out"
include(":app")
include(":core")
include(":engine")
include(":designsystem")
include(":codecpack")
