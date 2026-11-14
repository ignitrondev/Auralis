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

    // Backdrop blur and lens refraction for glass surfaces. Kept as implementation: no Backdrop
    // type is part of the design system API, so the library stays replaceable.
    implementation(libs.backdrop)

    // Artwork loading. Images come from YouTube image hosts only.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
