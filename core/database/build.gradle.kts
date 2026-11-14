plugins {
    alias(libs.plugins.auralis.android.library)
    alias(libs.plugins.auralis.android.room)
    alias(libs.plugins.auralis.hilt)
}

android {
    namespace = "com.ignitrondev.auralis.core.database"
}

dependencies {
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
