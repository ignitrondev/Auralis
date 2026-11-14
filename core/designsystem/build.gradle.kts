plugins {
    alias(libs.plugins.auralis.android.library)
    alias(libs.plugins.auralis.android.compose)
}

android {
    namespace = "com.ignitrondev.auralis.core.designsystem"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
}
