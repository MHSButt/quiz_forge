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
        // The OCR library (cz.adaptech.tesseract4android, used for scanned PDFs) is published
        // on Maven Central under that coordinate. If a future version of it ever moves back to
        // being JitPack-only, uncomment the line below and see the README's "How OCR works"
        // section for the alternate `com.github.adaptech-cz:Tesseract4Android`
        // dependency coordinate that goes with it.
        // maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QuizForge"
include(":app")
