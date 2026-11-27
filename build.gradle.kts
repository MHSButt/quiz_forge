// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.5.3" apply false
    // AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the app module no longer applies the
    // Kotlin Android plugin. Declaring it here only pins which Kotlin version AGP uses, keeping it
    // in step with the Compose compiler plugin below (the two versions must match).
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
