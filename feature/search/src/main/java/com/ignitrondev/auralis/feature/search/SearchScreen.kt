/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.GlassSearchBar
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.core.ui.CatalogRow
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.ErrorState
import com.ignitrondev.auralis.core.ui.LoadingState
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.core.ui.SectionTitle
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.SearchResults

@Composable
fun SearchDestination(
    actions: CatalogActions,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Opening a result also remembers the query.
    val trackingActions = actions.copy(
        openAlbum = { viewModel.onResultOpened(); actions.openAlbum(it) },
        openArtist = { viewModel.onResultOpened(); actions.openArtist(it) },
        openPlaylist = { viewModel.onResultOpened(); actions.openPlaylist(it) },
    )
    SearchScreen(
        state = state,
        actions = trackingActions,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onRemoveRecent = viewModel::onRemoveRecent,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    actions: CatalogActions,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AuralisTheme.spacing
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.feature_search_title),
            style = AuralisTheme.typography.screenTitle,
            color = AuralisTheme.colors.contentPrimary,
            modifier = Modifier.padding(start = spacing.screenGutter, end = spacing.screenGutter, top = top + spacing.xl),
        )
        GlassSearchBar(
            query = state.query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            placeholder = stringResource(R.string.feature_search_placeholder),
            modifier = Modifier.padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        )
        Box(modifier = Modifier.weight(1f)) {
            when (val results = state.results) {
                SearchResultsState.Idle -> RecentSearches(state.recentSearches, onSearch, onRemoveRecent)
                is SearchResultsState.Query -> when (val catalog = results.state) {
                    CatalogUiState.Loading -> LoadingState()
                    is CatalogUiState.Error -> ErrorState(catalog.reason, onRetry)
                    is CatalogUiState.Content -> if (catalog.data.isEmpty) {
                        MessageState(stringResource(R.string.feature_search_no_results, results.query))
                    } else {
                        Results(catalog.data, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(recent: List<String>, onSearch: (String) -> Unit, onRemove: (String) -> Unit) {
    if (recent.isEmpty()) {
        MessageState(stringResource(R.string.feature_search_hint))
        return
    }
    val spacing = AuralisTheme.spacing
    LazyColumn(contentPadding = PaddingValues(bottom = LocalBottomChromePadding.current + spacing.lg)) {
        item { SectionTitle(stringResource(R.string.feature_search_recent)) }
        items(recent, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearch(query) }
                    .padding(start = spacing.screenGutter, end = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = query,
                    style = AuralisTheme.typography.body,
                    color = AuralisTheme.colors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(query) }) {
                    Icon(
                        imageVector = AuralisIcons.Close,
                        contentDescription = stringResource(R.string.feature_search_remove_recent, query),
                        tint = AuralisTheme.colors.contentTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Results(results: SearchResults, actions: CatalogActions) {
    val spacing = AuralisTheme.spacing
    val songs = stringResource(R.string.feature_search_songs)
    val albums = stringResource(R.string.feature_search_albums)
    val artists = stringResource(R.string.feature_search_artists)
    val playlists = stringResource(R.string.feature_search_playlists)
    val top = stringResource(R.string.feature_search_top_result)
    LazyColumn(contentPadding = PaddingValues(bottom = LocalBottomChromePadding.current + spacing.lg)) {
        results.topResult?.let { best ->
            group(top, "top", listOf(best), actions, results)
        }
        group(songs, "songs", results.tracks, actions, results)
        group(artists, "artists", results.artists, actions, results)
        group(albums, "albums", results.albums, actions, results)
        group(playlists, "playlists", results.playlists, actions, results)
    }
}

private fun LazyListScope.group(
    title: String,
    key: String,
    items: List<CatalogItem>,
    actions: CatalogActions,
    results: SearchResults,
) {
    if (items.isEmpty()) return
    item(key = "title-$key") {
        SectionTitle(title, Modifier.padding(top = AuralisTheme.spacing.lg, bottom = AuralisTheme.spacing.xs))
    }
    items(items, key = { "$key-${it::class.simpleName}-${it.id}" }) { item ->
        CatalogRow(item = item, onClick = actions.onClick(item, results.tracks))
    }
}
