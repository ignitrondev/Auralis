/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.domain.update.AppInfo
import com.ignitrondev.auralis.domain.update.AppReleaseRepository
import com.ignitrondev.auralis.domain.update.AppVersion
import com.ignitrondev.auralis.domain.update.ReleaseNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How a release relates to the installed app. */
enum class ReleaseStatus {
    /** Newer than the installed version. */
    Newer,

    /** The installed version. */
    Installed,

    /** Older than the installed version. */
    Earlier,
}

/**
 * One version in the changelog.
 *
 * @property publishedAt An ISO 8601 instant, formatted by the screen for the user's locale.
 * @property downloadUrl The APK, shown as a button on the newest [ReleaseStatus.Newer] entry.
 */
data class ChangelogEntry(
    val version: String,
    val publishedAt: String?,
    val isPreRelease: Boolean,
    val notes: List<ReleaseNote>,
    val status: ReleaseStatus,
    val downloadUrl: String? = null,
)

sealed interface WhatsNewState {
    data object Loading : WhatsNewState
    data object Failed : WhatsNewState
    data class Content(val entries: List<ChangelogEntry>) : WhatsNewState
}

/**
 * The changelog: the notes of every Auralis release on GitHub, newest first. The IgniteTunes
 * releases in the same repository have no Auralis APK, so they are not listed.
 */
@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val releases: AppReleaseRepository,
    private val appInfo: AppInfo,
) : ViewModel() {

    private val _state = MutableStateFlow<WhatsNewState>(WhatsNewState.Loading)
    val state: StateFlow<WhatsNewState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _state.value = WhatsNewState.Loading
        viewModelScope.launch {
            _state.value = try {
                // A build without a proper version (a local build) counts as older than every release.
                val installed = AppVersion.parse(appInfo.versionName) ?: AppVersion.parse("0")!!
                val entries = releases.releases()
                    .filter { it.apkUrl != null }
                    .sortedByDescending { it.version }
                    .map { release ->
                        ChangelogEntry(
                            version = release.version.toString(),
                            publishedAt = release.publishedAt,
                            isPreRelease = release.version.isPreRelease,
                            notes = release.notes,
                            downloadUrl = release.apkUrl,
                            status = when {
                                release.version > installed -> ReleaseStatus.Newer
                                release.version == installed -> ReleaseStatus.Installed
                                else -> ReleaseStatus.Earlier
                            },
                        )
                    }
                WhatsNewState.Content(entries)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                WhatsNewState.Failed
            }
        }
    }
}
