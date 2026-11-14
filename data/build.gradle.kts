plugins {
    alias(libs.plugins.auralis.android.library)
    alias(libs.plugins.auralis.hilt)
}

android {
    namespace = "com.ignitrondev.auralis.data"
}

dependencies {
    api(projects.domain)
    implementation(projects.provider.api)
    implementation(projects.core.database)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}
