plugins {
    alias(libs.plugins.auralis.android.library)
}

android {
    namespace = "com.ignitrondev.auralis.provider.botguard"
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
