plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kishan.billorapos.feature.product.data"
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
    implementation(project(":core:database"))
    implementation(project(":feature:product:domain"))
    implementation(libs.androidx.core.ktx)
}
