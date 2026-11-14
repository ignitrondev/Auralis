plugins {
    alias(libs.plugins.auralis.android.library)
    alias(libs.plugins.auralis.android.compose)
    alias(libs.plugins.auralis.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ignitrondev.auralis.core.auth"
}

dependencies {
    api(projects.domain)
    api(projects.provider.api)
    implementation(projects.core.designsystem)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
