// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

val checkRupeeMojibake by tasks.registering {
    group = "verification"
    description = "Checks for raw rupee mojibake characters in source files"
    doLast {
        val mojibake = "â‚¹"
        var foundMojibake = false
        val directoriesToCheck = listOf("core", "feature", "app")
        directoriesToCheck.forEach { dirName ->
            val dir = file(dirName)
            if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                    if (file.readText().contains(mojibake)) {
                        println("Error: Mojibake sequence '$mojibake' found in file: ${file.absolutePath}")
                        foundMojibake = true
                    }
                }
            }
        }
        if (foundMojibake) {
            throw GradleException("Build failed due to corrupted Rupee mojibake strings. Please use Unicode escape sequence '\\u20B9' instead.")
        }
    }
}

subprojects {
    plugins.withId("com.android.application") {
        tasks.named("preBuild") {
            dependsOn(checkRupeeMojibake)
        }
    }
    plugins.withId("com.android.library") {
        tasks.named("preBuild") {
            dependsOn(checkRupeeMojibake)
        }
    }
}
