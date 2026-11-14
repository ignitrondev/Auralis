plugins {
    alias(libs.plugins.auralis.android.application)
    alias(libs.plugins.auralis.android.compose)
    alias(libs.plugins.auralis.hilt)
}

android {
    namespace = "com.ignitrondev.auralis"

    defaultConfig {
        applicationId = "com.ignitrondev.auralis"
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.designsystem)
    implementation(projects.data)
    implementation(projects.playback)
    implementation(projects.provider.youtube)
    implementation(projects.provider.botguard)
    implementation(projects.core.ui)
    implementation(projects.feature.browse)
    implementation(projects.feature.home)
    implementation(projects.feature.player)
    implementation(projects.feature.library)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Pinned: the 3.5.0 pulled in by ui-test-junit4 calls InputManager.getInstance, which is gone on Android 17.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
