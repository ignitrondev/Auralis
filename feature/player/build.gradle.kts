plugins {
    alias(libs.plugins.auralis.android.feature)
}

android {
    namespace = "com.ignitrondev.auralis.feature.player"
}

dependencies {
    implementation(projects.core.share)
}
