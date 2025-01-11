plugins {
    // Remember that we don't technically have to put anything here, but if we add more modules in the future, then this has some benefits
    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}
