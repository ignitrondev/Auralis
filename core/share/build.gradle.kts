plugins {
    alias(libs.plugins.auralis.android.library)
    alias(libs.plugins.auralis.android.compose)
}

android {
    namespace = "com.ignitrondev.auralis.core.share"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
