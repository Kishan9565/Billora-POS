plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kishan.billorapos.feature.shop.domain"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
}
