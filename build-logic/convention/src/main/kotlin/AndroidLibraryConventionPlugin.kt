/*
 * Copyright (c) 2026 IgnitronDev
 */
import com.android.build.api.dsl.LibraryExtension
import com.ignitrondev.auralis.buildlogic.AuralisJavaVersion
import com.ignitrondev.auralis.buildlogic.intVersion
import com.ignitrondev.auralis.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.intVersion("compile-sdk")
                defaultConfig {
                    minSdk = libs.intVersion("min-sdk")
                    consumerProguardFiles("consumer-rules.pro")
                }
                compileOptions {
                    sourceCompatibility = AuralisJavaVersion
                    targetCompatibility = AuralisJavaVersion
                }
            }

            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
            }
        }
    }
}
