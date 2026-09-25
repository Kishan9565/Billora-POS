plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kishan.billorapos.core.data"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(project(":core:database"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
}
