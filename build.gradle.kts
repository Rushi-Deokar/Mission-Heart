// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false // ✅ Yeh line Firebase ke liye add ki hai

    // ✅ KSP ab directly libs.versions.toml se clean tarike se aayega
    alias(libs.plugins.ksp) apply false
}