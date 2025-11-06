plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" // Add this line
}

android {
    namespace = "vcmsa.projects.district49_android"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
        compose = true
    }

    defaultConfig {
        applicationId = "vcmsa.projects.district49_android"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- Firebase: use BOM and unversioned artifacts ---
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // App Check (Play Integrity) – correct artifact (no -ktx variant)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // MP Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // JSON
    implementation("org.json:json:20230227")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // === Compose Dependencies ===
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    
    // === Glide Dependencies ===
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // For PDF handling (if needed)
    implementation("com.itextpdf:itext7-core:7.2.5")
    //Image loading in compose
    implementation("io.coil-kt:coil-compose:2.5.0")

    //for push notifctaion firestore function
    implementation("com.google.firebase:firebase-messaging-ktx")

    // for grid layouts
    implementation("androidx.gridlayout:gridlayout:1.0.0")


}