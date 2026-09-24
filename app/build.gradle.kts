plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kishan.billorapos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kishan.billorapos"
        minSdk = 24
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
    kotlinOptions { jvmTarget = "11" }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Modules
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:printer"))
    implementation(project(":feature:billing:domain"))
    implementation(project(":feature:billing:presentation"))
    implementation(project(":feature:product:domain"))
    implementation(project(":feature:product:data"))
    implementation(project(":feature:product:presentation"))
    implementation(project(":feature:shop:domain"))
    implementation(project(":feature:shop:data"))
    implementation(project(":feature:shop:presentation"))
    implementation(project(":feature:settings:domain"))
    implementation(project(":feature:settings:data"))
    implementation(project(":feature:settings:presentation"))

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    add("kapt", libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Barcode Scanning
    implementation(libs.google.mlkit.barcode.scanning)

    // ZXing
    implementation(libs.zxing.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
