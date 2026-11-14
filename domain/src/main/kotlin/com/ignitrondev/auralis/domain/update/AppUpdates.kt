/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.update

import kotlinx.coroutines.flow.StateFlow

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

/**
 * Keeps track of whether a newer Auralis is available, for the update dot on the Settings tab and
 * the update card in Settings.
 */
interface AppUpdates {
    /**
     * The newest update found, or null. It starts from the last result saved on the device, so the
     * dot survives a restart while offline, and it is dropped once that version is installed.
     */
    val available: StateFlow<AppRelease?>

    /** Checks now, for the Settings button. Returns the update, or null when up to date. Throws when the check fails. */
    suspend fun checkNow(): AppRelease?

    /** Checks only when the last check is old enough. Failures are ignored: it runs in the background. */
    suspend fun checkIfDue()
}

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
