/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.core.ui.CatalogRow
import com.ignitrondev.auralis.core.ui.SectionCarousel
import com.ignitrondev.auralis.core.ui.SectionTitle
import com.ignitrondev.auralis.core.ui.formatDuration

@Composable
fun AlbumDestination(
    actions: CatalogActions,
    onBack: () -> Unit,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    DetailScaffold(state = state, onBack = onBack, onRetry = viewModel::retry) { detail ->
        item(key = "header") {
            DetailHeader(
                artworkUri = detail.album.artworkUri,
                title = detail.album.title,
                lines = listOfNotNull(
                    listOfNotNull(detail.album.artistNames.ifBlank { null }, detail.album.year).joinToString(" • "),
                    detail.trackCountText,
                ),
            )
        }
        if (save.visible) {
            item(key = "save") { SaveToLibrary(save = save, onToggle = viewModel::toggleSaved) }
        }
        itemsIndexed(detail.tracks, key = { index, track -> "$index-${track.id}" }) { index, track ->
            CatalogRow(
                item = track,
                onClick = actions.onClick(track, detail.tracks),
                subtitle = track.artistNames,
                leadingNumber = index + 1,
                trailing = track.durationMs?.let(::formatDuration),
            )
        }
        detail.description?.let { description ->
            item(key = "description") { Description(description) }
        }
    }
}

@Composable
fun ArtistDestination(
    actions: CatalogActions,
    onBack: () -> Unit,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(state = state, onBack = onBack, onRetry = viewModel::retry) { detail ->
        item(key = "header") {
            DetailHeader(
                artworkUri = detail.artist.artworkUri,
                title = detail.artist.name,
                lines = listOfNotNull(detail.artist.subtitle),
                circular = true,
            )
        }
        if (detail.topTracks.isNotEmpty()) {
            item(key = "top-title") {
                SectionTitle(stringResource(R.string.feature_browse_top_songs), Modifier.padding(top = AuralisTheme.spacing.lg))
            }
            items(detail.topTracks, key = { "top-${it.id}" }) { track ->
                CatalogRow(item = track, onClick = actions.onClick(track, detail.topTracks))
            }
        }
        detail.sections.forEachIndexed { index, section ->
            item(key = "section-$index") {
                Spacer(Modifier.height(AuralisTheme.spacing.xl))
                SectionCarousel(section = section, actions = actions)
            }
        }
        detail.description?.let { description ->
            item(key = "description") { Description(description) }
        }
    }
}

@Composable
fun PlaylistDestination(
    actions: CatalogActions,
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(state = state, onBack = onBack, onRetry = viewModel::retry) { detail ->
        item(key = "header") {
            DetailHeader(
                artworkUri = detail.playlist.artworkUri,
                title = detail.playlist.title,
                lines = listOfNotNull(detail.playlist.author, detail.trackCountText),
            )
        }
        itemsIndexed(detail.tracks, key = { index, track -> "$index-${track.id}" }) { _, track ->
            CatalogRow(
                item = track,
                onClick = actions.onClick(track, detail.tracks),
                subtitle = track.artistNames,
                trailing = track.durationMs?.let(::formatDuration),
            )
        }
        if (detail.hasMore) {
            item(key = "more") { Description(pluralStringResource(R.plurals.feature_browse_more_tracks, detail.tracks.size, detail.tracks.size)) }
        }
    }
}

@Composable
private fun Description(text: String) {
    Text(
        text = text,
        style = AuralisTheme.typography.bodySmall,
        color = AuralisTheme.colors.contentTertiary,
        modifier = Modifier.padding(horizontal = AuralisTheme.spacing.screenGutter, vertical = AuralisTheme.spacing.xl),
    )
}

/** Save state is shown by icon and label (check vs plus), never by colour (spec 8.2.1). */
@Composable
private fun SaveToLibrary(save: SaveUiState, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AuralisTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
    ) {
        GlassButton(
            text = stringResource(if (save.saved) R.string.feature_browse_in_library else R.string.feature_browse_save_to_library),
            icon = if (save.saved) AuralisIcons.Saved else AuralisIcons.Add,
            onClick = onToggle,
            enabled = !save.pending,
        )
        if (save.failed) {
            Text(
                text = stringResource(R.string.feature_browse_save_failed),
                style = AuralisTheme.typography.bodySmall,
                color = AuralisTheme.colors.contentSecondary,
            )
        }
    }
}
