import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)
}

val javafxPlatform = when {
    OperatingSystem.current().isWindows -> "win"
    OperatingSystem.current().isMacOsX -> "mac"
    else -> "linux"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.provider.youtube)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.ktor.client.java)
    implementation("org.openjfx:javafx-media:${libs.versions.javafx.get()}:$javafxPlatform")
    implementation("org.openjfx:javafx-graphics:${libs.versions.javafx.get()}:$javafxPlatform")
    implementation("org.openjfx:javafx-base:${libs.versions.javafx.get()}:$javafxPlatform")
}

compose.desktop {
    application {
        mainClass = "com.ignitrondev.auralis.desktop.MainKt"
        nativeDistributions {
            packageName = "Auralis"
            // The release workflow passes the tag's version; an MSI version is three numbers.
            packageVersion = providers.gradleProperty("auralisDesktopVersion").getOrElse("2.1.0")
            // The whole Java runtime. The tool that guesses the needed modules misses java.net.http,
            // which the network client loads, and a missing module only fails once the app runs.
            includeAllModules = true
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            windows {
                menuGroup = "Auralis"
                upgradeUuid = "c87975fc-519a-4f7a-b93f-19d8a535cf53"
            }
        }
    }
}
