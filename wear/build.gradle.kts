plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sensor.extractor"
    compileSdk = 35
    compileSdkExtension = 15

    defaultConfig {
        applicationId = "com.sensor.extractor"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation("androidx.core:core-ktx:1.13.1")

    // ADD THIS LINE for the splash screen theme to work:
    implementation("androidx.core:core-splashscreen:1.0.1")

    // CoroutineScope, Dispatchers.IO, SupervisorJob, launch
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ActivityResultContracts.RequestMultiplePermissions (permission launcher in MainActivity)
    implementation("androidx.activity:activity-ktx:1.9.3")

    implementation("com.google.android.gms:play-services-wearable:18.2.0")
}