plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kishan.billorapos.feature.settings.domain"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:domain"))
    implementation(project(":core:printer"))
    implementation(libs.androidx.core.ktx)
}
