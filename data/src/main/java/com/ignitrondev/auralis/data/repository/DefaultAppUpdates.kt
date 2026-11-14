/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import android.content.Context
import com.ignitrondev.auralis.domain.update.AppInfo
import com.ignitrondev.auralis.domain.update.AppRelease
import com.ignitrondev.auralis.domain.update.AppReleaseRepository
import com.ignitrondev.auralis.domain.update.AppUpdates
import com.ignitrondev.auralis.domain.update.AppVersion
import com.ignitrondev.auralis.domain.update.findUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** What the update check keeps between launches. */
internal interface UpdateCheckStore {
    var lastCheckedAtMs: Long
    var saved: AppRelease?
}

/**
 * Checks GitHub Releases for a newer Auralis: on app start at most every [CHECK_INTERVAL_MS], and
 * whenever the user asks in Settings. The result is saved, so the update dot stays after a restart
 * without a network, and it disappears once that version is installed.
 */
@Singleton
class DefaultAppUpdates internal constructor(
    appInfo: AppInfo,
    private val releases: AppReleaseRepository,
    private val store: UpdateCheckStore,
    private val clock: () -> Long,
) : AppUpdates {

    @Inject
    constructor(
        appInfo: AppInfo,
        releases: AppReleaseRepository,
        @ApplicationContext context: Context,
    ) : this(appInfo, releases, PreferencesUpdateCheckStore(context), System::currentTimeMillis)

    // A build without a proper version (a local build) counts as older than every release.
    private val installed = AppVersion.parse(appInfo.versionName) ?: AppVersion.parse("0")!!
    private val lock = Mutex()
    private val _available = MutableStateFlow(store.saved?.takeIf { it.version > installed })
    override val available: StateFlow<AppRelease?> = _available.asStateFlow()

    override suspend fun checkNow(): AppRelease? = lock.withLock {
        val update = findUpdate(installed, releases.releases())
        store.lastCheckedAtMs = clock()
        store.saved = update
        _available.value = update
        update
    }

    override suspend fun checkIfDue() {
        val last = store.lastCheckedAtMs
        // Never checked, or the clock moved back (set by hand): check now rather than wait.
        val recent = last != 0L && clock() - last in 0 until CHECK_INTERVAL_MS
        if (recent) return
        try {
            checkNow()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Offline or rate-limited: keep what is known and try again next launch.
        }
    }

    internal companion object {
        const val CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L
    }
}

/** Stores the last check in the app's private preferences. Holds only public release data. */
private class PreferencesUpdateCheckStore(context: Context) : UpdateCheckStore {
    private val preferences = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)

    override var lastCheckedAtMs: Long
        get() = preferences.getLong(KEY_CHECKED_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_CHECKED_AT, value).apply()

    override var saved: AppRelease?
        get() {
            val version = preferences.getString(KEY_VERSION, null)?.let(AppVersion::parse) ?: return null
            val page = preferences.getString(KEY_PAGE, null) ?: return null
            return AppRelease(version, page, preferences.getString(KEY_APK, null))
        }
        set(value) {
            val editor = preferences.edit()
            if (value == null) {
                editor.remove(KEY_VERSION).remove(KEY_PAGE).remove(KEY_APK)
            } else {
                editor.putString(KEY_VERSION, value.version.toString())
                    .putString(KEY_PAGE, value.pageUrl)
                    .putString(KEY_APK, value.apkUrl)
            }
            editor.apply()
        }

    private companion object {
        const val KEY_CHECKED_AT = "checked_at_ms"
        const val KEY_VERSION = "update_version"
        const val KEY_PAGE = "update_page_url"
        const val KEY_APK = "update_apk_url"
    }
}
