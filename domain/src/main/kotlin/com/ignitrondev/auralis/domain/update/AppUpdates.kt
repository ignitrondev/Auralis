/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.update

/** The installed app, as the update check needs it. */
data class AppInfo(val versionName: String)

/**
 * A published release of Auralis.
 *
 * @property pageUrl The release page, with notes.
 * @property apkUrl The APK to download, or null when the release has none attached yet.
 */
data class AppRelease(
    val version: AppVersion,
    val pageUrl: String,
    val apkUrl: String?,
)

/** Where Auralis releases are published. Calls fail with an exception when they cannot be read. */
interface AppReleaseRepository {
    suspend fun releases(): List<AppRelease>
}

/**
 * The release to offer as an update, or null when [installed] is already the newest. A beta
 * install is offered betas and releases; a release install is offered releases only, so nobody
 * moves to a beta without choosing it. Releases without an APK are skipped.
 */
fun findUpdate(installed: AppVersion, releases: List<AppRelease>): AppRelease? =
    releases
        .filter { it.apkUrl != null }
        .filter { installed.isPreRelease || !it.version.isPreRelease }
        .maxByOrNull { it.version }
        ?.takeIf { it.version > installed }
