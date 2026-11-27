import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    // No "org.jetbrains.kotlin.android" here: AGP 9 has Kotlin support built in.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hassan.quizforge"
    // Compose 1.13 / AndroidX libraries need Android 37.1+ APIs, so compile against 37.2
    // (a minor SDK release). This doesn't change which phones can run the app - that's minSdk.
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.hassan.quizforge"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // Kept off by default so the generated project builds and runs with zero extra
            // configuration. Turn this on (and extend proguard-rules.pro if needed) once you're
            // ready to ship a shrunk release build.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Core / lifecycle
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.1")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.11.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coroutines (used for offloading file parsing + question generation off the main thread)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Offline PDF text extraction. Pure-Kotlin/Java, no network calls, no Google Play Services.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Offline OCR for scanned/image-only PDF pages. Pure on-device native Tesseract - no Play
    // Services, no network, no dynamically-downloaded model (that's exactly why this was chosen
    // over ML Kit, which requires both). See the README's "How OCR works" section for details.
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
