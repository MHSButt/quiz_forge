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
        // via JitPack, not Maven Central, even though its group id looks like a normal Maven
        // Central coordinate - this repository is required for that one dependency to resolve.
        // See the README's "How OCR works" section for details.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QuizForge"
include(":app")
