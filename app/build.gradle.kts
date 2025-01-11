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
        versionCode = 400
        versionName = "1.40"
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
    // TODO do we need this include to grab jars? We don't even use jars for dependencies
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    implementation("com.android.support:multidex:2.0.1")
    //noinspection GradleDependency

    // materialDrawerVersion = '8.0.1' // we'll eventually have to migrate: https://github.com/mikepenz/MaterialDrawer/blob/develop/MIGRATION.md
    implementation("com.mikepenz:materialdrawer:7.0.0")

    implementation("androidx.recyclerview:recyclerview:1.2.1")

    //noinspection DifferentStdlibGradleVersion)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    // TODO move to libs.versions.toml
    val solarthingVersion = "03de524854c55d6389cdefbde96351ac0f422586".substring(0, 10)

    //noinspection GradleDependency // Since we use pre-releases, we don't need Android Studio warning us about this
    implementation("com.github.wildmountainfarms.solarthing:core:$solarthingVersion")
    // Note that Gradle cannot always tell that it needs to bump this couchdb-java version, so if you don't bump it to
    //   match whatever couchdb-java version solarthing:core is using, you might get problems because a new method might not be found
    //noinspection GradleDependency
    //implementation "com.github.retrodaredevil:couchdb-java:${"d41524674af822438430d5228550c3caff0ab145".substring(0, 10)}"

    // TODO move to libs.versions.toml
    val jacksonVersion = "2.13.1" // https://github.com/FasterXML/jackson-databind/releases
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion") // { changing = true } // uncomment if we use a snapshot version of Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    val slf4jVersion = "1.7.32" // http://www.slf4j.org/download.html
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-android
    implementation("org.slf4j:slf4j-android:$slf4jVersion")

//    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.1'
}
