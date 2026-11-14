/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.core.ui.CatalogRow
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.ErrorState
import com.ignitrondev.auralis.core.ui.LoadingState
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.core.ui.SectionCarousel
import com.ignitrondev.auralis.core.ui.SectionTitle
import com.ignitrondev.auralis.domain.model.AlbumRef
import com.ignitrondev.auralis.domain.model.ArtistRef
import com.ignitrondev.auralis.domain.model.CatalogSection
import com.ignitrondev.auralis.domain.model.HistorySection
import com.ignitrondev.auralis.domain.model.LibraryCollection
import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicPlaylist
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository

@Composable
fun LibraryDestination(
    actions: CatalogActions,
    onSignIn: () -> Unit,
    onOpenCollection: (LibraryCollection) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        actions = actions,
        onSignIn = onSignIn,
        onRetry = viewModel::retry,
        onOpenCollection = onOpenCollection,
        modifier = modifier,
    )
}

@Composable
fun LibraryScreen(
    state: CatalogUiState<LibraryContent>,
    actions: CatalogActions,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    onOpenCollection: (LibraryCollection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(modifier = modifier.fillMaxSize().padding(top = top + AuralisTheme.spacing.xl)) {
        Text(
            text = stringResource(R.string.feature_library_title),
            style = AuralisTheme.typography.screenTitle,
            color = AuralisTheme.colors.contentPrimary,
            modifier = Modifier
                .padding(horizontal = AuralisTheme.spacing.screenGutter)
                .semantics { heading() },
        )
        when (state) {
            CatalogUiState.Loading -> LoadingState()
            is CatalogUiState.Error -> if (state.reason == CatalogException.Reason.SignInRequired) {
                MessageState(message = stringResource(R.string.feature_library_sign_in_message)) {
                    GlassButton(text = stringResource(R.string.feature_library_sign_in), onClick = onSignIn)
                }
            } else {
                ErrorState(state.reason, onRetry)
            }
            is CatalogUiState.Content -> LibraryContentList(content = state.data, actions = actions, onOpenCollection = onOpenCollection)
        }
    }
}

@Composable
private fun LibraryContentList(content: LibraryContent, actions: CatalogActions, onOpenCollection: (LibraryCollection) -> Unit) {
    val spacing = AuralisTheme.spacing
    val likedMusic = MusicPlaylist(
        id = LibraryRepository.LIKED_MUSIC_PLAYLIST_ID,
        sourceId = "youtube",
        title = stringResource(R.string.feature_library_liked_music),
        author = null,
        artworkUri = null,
    )
    val titles = LibraryCollection.entries.associateWith { stringResource(it.title) }
    val emptyHistory = stringResource(R.string.feature_library_history_empty)
    LazyColumn(
        contentPadding = PaddingValues(top = spacing.lg, bottom = LocalBottomChromePadding.current + spacing.xl),
    ) {
        item(key = "liked") {
            CatalogRow(
                item = likedMusic,
                onClick = actions.onClick(likedMusic),
                subtitle = stringResource(R.string.feature_library_liked_music_subtitle),
                leadingIcon = AuralisIcons.Liked,
            )
        }
        // Playlists, albums, artists: the first page of each, with See all for the rest.
        LibraryCollection.entries.forEach { collection ->
            val items = content.items(collection)
            if (items.isNotEmpty()) {
                item(key = collection.name) {
                    SectionCarousel(
                        section = CatalogSection(titles.getValue(collection), items),
                        actions = actions,
                        modifier = Modifier.padding(top = spacing.xl),
                        onSeeAll = { onOpenCollection(collection) },
                    )
                }
            }
        }
        item(key = "history-heading") {
            SectionTitle(
                text = stringResource(R.string.feature_library_recently_played),
                modifier = Modifier.padding(top = spacing.xl),
            )
        }
        if (content.history.isEmpty) {
            item(key = "history-empty") {
                Text(
                    text = emptyHistory,
                    style = AuralisTheme.typography.body,
                    color = AuralisTheme.colors.contentSecondary,
                    modifier = Modifier.padding(horizontal = spacing.screenGutter, vertical = spacing.md),
                )
            }
        } else {
            history(content.history, actions)
        }
    }
}

private fun LazyListScope.history(history: ListeningHistory, actions: CatalogActions) {
    history.sections.forEachIndexed { sectionIndex, section ->
        section.title?.let { title ->
            // The period ("Today", "Last week") as a small label under the section heading.
            item(key = "title:$sectionIndex") {
                Text(
                    text = title,
                    style = AuralisTheme.typography.label,
                    color = AuralisTheme.colors.contentSecondary,
                    modifier = Modifier
                        .padding(horizontal = AuralisTheme.spacing.screenGutter)
                        .padding(top = AuralisTheme.spacing.lg, bottom = AuralisTheme.spacing.xs)
                        .semantics { heading() },
                )
            }
        }
        // The same track can appear in several periods, so keys include the position.
        itemsIndexed(section.tracks, key = { index, track -> "$sectionIndex:$index:${track.id}" }) { _, track ->
            // Tapping plays the rest of that period as the queue.
            CatalogRow(item = track, onClick = actions.onClick(track, section.tracks))
        }
    }
}

@Preview
@Composable
private fun LibraryScreenPreview() {
    val track = MusicTrack("1", "youtube", "Teh Hijau", listOf(ArtistRef(null, "Tulus")), AlbumRef(null, "Manusia"), 212_000, null)
    val album = MusicAlbum("MPRE1", "youtube", "Manusia", listOf(ArtistRef(null, "Tulus")), "2022", null)
    AuralisTheme {
        LibraryScreen(
            state = CatalogUiState.Content(
                LibraryContent(
                    mapOf(LibraryCollection.ALBUMS to listOf(album)),
                    ListeningHistory(listOf(HistorySection("Today", listOf(track)))),
                ),
            ),
            actions = CatalogActions.None,
            onSignIn = {},
            onRetry = {},
            onOpenCollection = {},
        )
    }
}
