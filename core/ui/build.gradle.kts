plugins {
    alias(libs.plugins.auralis.android.library)
    alias(libs.plugins.auralis.android.compose)
}

android {
    namespace = "com.ignitrondev.auralis.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.domain)
}
