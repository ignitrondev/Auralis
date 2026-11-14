/*
 * Copyright (c) 2026 IgnitronDev
 */
import com.ignitrondev.auralis.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Base setup for every feature module: Android library, Compose, Hilt ViewModels, the design
 * system, shared catalog UI, the domain, and type-safe Navigation Compose routes.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "auralis.android.library")
            apply(plugin = "auralis.android.compose")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
            apply(plugin = "auralis.hilt")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":domain"))
                add("implementation", libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("kotlinx-serialization-core").get())
            }
        }
    }
}
