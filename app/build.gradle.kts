plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hassan.quizforge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hassan.quizforge"
        minSdk = 26
        targetSdk = 35
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

    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    // Core / lifecycle
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coroutines (used for offloading file parsing + question generation off the main thread)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Offline PDF text extraction. Pure-Kotlin/Java, no network calls, no Google Play Services.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Offline OCR for scanned/image-only PDF pages. Pure on-device native Tesseract - no Play
    // Services, no network, no dynamically-downloaded model (that's exactly why this was chosen
    // over ML Kit, which requires both). See the README's "How OCR works" section for details.
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
