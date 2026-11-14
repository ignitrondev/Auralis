plugins {
    alias(libs.plugins.auralis.android.library)
    alias(libs.plugins.auralis.hilt)
}

android {
    namespace = "com.ignitrondev.auralis.playback"
}

dependencies {
    api(projects.domain)
    implementation(projects.provider.api)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
