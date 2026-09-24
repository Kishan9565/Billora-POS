plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kishan.billorapos.feature.settings.presentation"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:printer"))
    implementation(project(":feature:settings:domain"))
    implementation(project(":feature:shop:domain"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.koin.androidx.compose)
}
