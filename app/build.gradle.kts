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
    implementation(projects.core.designsystem)
    implementation(projects.feature.home)

    implementation(libs.androidx.activity.compose)
}
