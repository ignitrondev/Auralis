plugins {
    alias(libs.plugins.auralis.android.feature)
}

android {
    namespace = "com.ignitrondev.auralis.feature.settings"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.share)
}
