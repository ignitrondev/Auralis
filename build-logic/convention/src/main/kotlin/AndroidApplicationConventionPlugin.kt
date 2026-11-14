/*
 * Copyright (c) 2026 IgnitronDev
 */
import com.android.build.api.dsl.ApplicationExtension
import com.ignitrondev.auralis.buildlogic.AuralisJavaVersion
import com.ignitrondev.auralis.buildlogic.intVersion
import com.ignitrondev.auralis.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.intVersion("compile-sdk")
                defaultConfig {
                    minSdk = libs.intVersion("min-sdk")
                    targetSdk = libs.intVersion("target-sdk")
                }
                compileOptions {
                    sourceCompatibility = AuralisJavaVersion
                    targetCompatibility = AuralisJavaVersion
                }
            }
        }
    }
}
