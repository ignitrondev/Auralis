import java.util.Properties

plugins {
    alias(libs.plugins.auralis.android.library)
}

/**
 * The YouTube web client key used by the BotGuard requests is never committed (spec 17.3).
 * It comes from the environment (CI secret) or from the git-ignored local.properties:
 *   auralis.youtubeWebApiKey=<key>
 * Without it, BotGuard is disabled and signed-in playback still works.
 */
val youtubeWebApiKey: String = providers.environmentVariable("AURALIS_YOUTUBE_WEB_API_KEY").orNull
    ?: Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
    }.getProperty("auralis.youtubeWebApiKey")
    ?: ""

android {
    namespace = "com.ignitrondev.auralis.provider.botguard"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "YOUTUBE_WEB_API_KEY", "\"$youtubeWebApiKey\"")
    }
}

dependencies {
    // Only the Auralis AttestationProvider contract: no InnerTubeX type is used here.
    implementation(projects.provider.youtube)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
