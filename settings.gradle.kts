pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Billora POS"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:presentation")
include(":core:design-system")
include(":core:printer")
include(":feature:billing:domain")
include(":feature:billing:presentation")
include(":feature:product:domain")
include(":feature:product:data")
include(":feature:product:presentation")
include(":feature:shop:domain")
include(":feature:shop:data")
include(":feature:shop:presentation")
include(":feature:settings:domain")
include(":feature:settings:data")
include(":feature:settings:presentation")
