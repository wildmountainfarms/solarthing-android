plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "me.retrodaredevil.solarthing.android"
    sourceSets.getByName("main").java.filter.exclude("META-INF/**/*")
    compileSdk = 34
    defaultConfig {
        multiDexEnabled = true
        applicationId = "me.retrodaredevil.solarthing.android"
        minSdk = 26
        // NOTE: Android 14 is SDK level 34 - https://apilevels.com/
        targetSdk = 34
        versionCode = 420
        versionName = "1.42"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    packaging {
        resources {
            excludes.addAll(listOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/notice.txt", "META-INF/ASL2.0"))
        }
    }
    compileOptions {
//        coreLibraryDesugaringEnabled true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        // https://kotlinlang.org/docs/java-interop.html#jsr-305-support
        freeCompilerArgs += listOf("-Xjsr305=strict")
    }

    buildFeatures {
        // Determines whether to generate binder classes for your AIDL files.
        aidl = false
        // Determines whether to support RenderScript.
        renderScript = false
        // Determines whether to support injecting custom variables into the module’s R class.
        resValues = true
        // Determines whether to support shader AOT compilation.
        shaders = false
    }
    lint {
        abortOnError = false
    }
}
// NOTE: Repositories configured in settings.gradle.kts

dependencies {
    implementation(libs.multidex)

    // materialDrawerVersion = '8.0.1'
    implementation(libs.materialdrawer)

    implementation(libs.androidx.recyclerview)

    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.solarthingCore)
    // Note that Gradle cannot always tell that it needs to bump this couchdb-java version, so if you don't bump it to
    //   match whatever couchdb-java version solarthing:core is using, you might get problems because a new method might not be found
    //noinspection GradleDependency
    //implementation "com.github.retrodaredevil:couchdb-java:${"d41524674af822438430d5228550c3caff0ab145".substring(0, 10)}"

    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind) // { changing = true } // uncomment if we use a snapshot version of Jackson
    implementation(libs.jackson.module.kotlin)

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.android)

//    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.1'
}
