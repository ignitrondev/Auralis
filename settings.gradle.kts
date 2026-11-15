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
include(":core:auth")
include(":core:database")
include(":core:designsystem")
include(":core:share")
include(":core:ui")
include(":data")
include(":domain")
include(":feature:browse")
include(":feature:home")
include(":feature:player")
include(":feature:library")
include(":feature:search")
include(":feature:settings")
include(":playback")
include(":provider:api")
include(":provider:botguard")
include(":provider:youtube")
