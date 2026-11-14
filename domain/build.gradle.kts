plugins {
    alias(libs.plugins.auralis.jvm.library)
}

dependencies {
    // Flow for observable repository data. Pure Kotlin: no Android dependency.
    api(libs.kotlinx.coroutines.core)
}
