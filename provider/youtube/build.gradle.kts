plugins {
    alias(libs.plugins.auralis.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.domain)

    // InnerTubeX is wrapped by this module only (spec section 6.2): no InnerTubeX type may
    // appear outside :provider:youtube, so the library stays replaceable.
    implementation(libs.innertubex)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    // Live calls to YouTube Music run only when asked for: ./gradlew :provider:youtube:test -Plive
    // They are never part of the normal test run (spec section 13 uses recorded fixtures).
    systemProperty("auralis.live", project.hasProperty("live").toString())
    systemProperty("auralis.fixtures", file("src/test/resources/fixtures").absolutePath)
    systemProperty("auralis.recordFixtures", project.hasProperty("recordFixtures").toString())
    testLogging { showStandardStreams = project.hasProperty("live") }
}
