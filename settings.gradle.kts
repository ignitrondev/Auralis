pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // InnerTubeX and its QuackJS dependency are published only through JitPack.
        // Restrict JitPack to those two groups so no other dependency can resolve from it.
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.MetrolistGroup.innertubex")
                includeGroup("com.github.MetrolistGroup.quackjs")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Auralis"

include(":app")
include(":core:designsystem")
include(":domain")
include(":feature:home")
include(":feature:library")
include(":feature:search")
include(":feature:settings")
include(":provider:youtube")
