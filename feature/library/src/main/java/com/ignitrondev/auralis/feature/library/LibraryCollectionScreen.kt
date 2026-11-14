/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.core.ui.CatalogCard
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.ErrorState
import com.ignitrondev.auralis.core.ui.LoadingState
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.domain.model.LibraryCollection
import com.ignitrondev.auralis.core.ui.R as UiR

@Composable
fun LibraryCollectionDestination(
    actions: CatalogActions,
    onBack: () -> Unit,
    viewModel: LibraryCollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryCollectionScreen(
        collection = viewModel.collection,
        state = state,
        actions = actions,
        onBack = onBack,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
    )
}

/** Everything of one kind in a grid; the next page loads as the end of the list comes into view. */
@Composable
fun LibraryCollectionScreen(
    collection: LibraryCollection,
    state: CatalogUiState<CollectionItems>,
    actions: CatalogActions,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val spacing = AuralisTheme.spacing
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            CatalogUiState.Loading -> LoadingState()
            is CatalogUiState.Error -> ErrorState(state.reason, onRetry)
            is CatalogUiState.Content -> if (state.data.items.isEmpty()) {
                MessageState(stringResource(collection.emptyText))
            } else {
                ItemGrid(collection = collection, items = state.data, actions = actions, onLoadMore = onLoadMore)
            }
        }
        GlassIconButton(
            icon = AuralisIcons.Back,
            contentDescription = stringResource(UiR.string.core_ui_back),
            onClick = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(spacing.sm),
        )
    }
}

@Composable
private fun ItemGrid(collection: LibraryCollection, items: CollectionItems, actions: CatalogActions, onLoadMore: () -> Unit) {
    val spacing = AuralisTheme.spacing
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val grid = rememberLazyGridState()
    // Ask for the next page a few rows before the end, so scrolling rarely has to wait.
    val nearEnd by remember {
        derivedStateOf {
            val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= grid.layoutInfo.totalItemsCount - LOAD_AHEAD_ITEMS
        }
    }
    LaunchedEffect(nearEnd, items.canLoadMore) {
        if (nearEnd && items.canLoadMore && !items.loadMoreFailed) onLoadMore()
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = CardMinWidth),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            // Below the back button, which floats over the top-left corner.
            top = top + BackButtonArea + spacing.md,
            bottom = LocalBottomChromePadding.current + spacing.xl,
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(collection.title),
                style = AuralisTheme.typography.screenTitle,
                color = AuralisTheme.colors.contentPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
        items(items.items, key = { it::class.simpleName + it.id }) { item ->
            CatalogCard(item = item, onClick = actions.onClick(item), fillWidth = true)
        }
        if (items.loadingMore || items.loadMoreFailed) {
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (items.loadMoreFailed) {
                        GlassButton(
                            text = stringResource(R.string.feature_library_load_more_failed),
                            icon = AuralisIcons.Retry,
                            onClick = onLoadMore,
                        )
                    } else {
                        CircularProgressIndicator(
                            color = AuralisTheme.colors.contentSecondary,
                            trackColor = AuralisTheme.colors.selection,
                            strokeWidth = 3.dp,
                        )
                    }
                }
            }
        }
    }
}

/** The heading of a collection, in Library and on its own page. */
internal val LibraryCollection.title: Int
    get() = when (this) {
        LibraryCollection.PLAYLISTS -> R.string.feature_library_playlists
        LibraryCollection.ALBUMS -> R.string.feature_library_albums
        LibraryCollection.ARTISTS -> R.string.feature_library_artists
    }

private val LibraryCollection.emptyText: Int
    get() = when (this) {
        LibraryCollection.PLAYLISTS -> R.string.feature_library_playlists_empty
        LibraryCollection.ALBUMS -> R.string.feature_library_albums_empty
        LibraryCollection.ARTISTS -> R.string.feature_library_artists_empty
    }

private val CardMinWidth = 148.dp
private val BackButtonArea = 72.dp
private const val LOAD_AHEAD_ITEMS = 6
