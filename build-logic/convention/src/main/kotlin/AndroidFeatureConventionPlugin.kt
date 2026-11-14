/*
 * Copyright (c) 2026 IgnitronDev
 */
import com.ignitrondev.auralis.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** Base setup for every feature module: Android library + Compose + design system. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "auralis.android.library")
            apply(plugin = "auralis.android.compose")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            }
        }
    }
}
