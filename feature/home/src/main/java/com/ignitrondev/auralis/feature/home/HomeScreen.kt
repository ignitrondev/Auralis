/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.ErrorState
import com.ignitrondev.auralis.core.ui.LoadingState
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.core.ui.SectionCarousel
import com.ignitrondev.auralis.domain.model.HomeFeed
import java.time.LocalTime

@Composable
fun HomeDestination(
    actions: CatalogActions,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, actions = actions, onRetry = viewModel::retry, modifier = modifier)
}

@Composable
fun HomeScreen(
    state: CatalogUiState<HomeFeed>,
    actions: CatalogActions,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CatalogUiState.Loading -> LoadingState(modifier)
        is CatalogUiState.Error -> ErrorState(state.reason, onRetry, modifier)
        is CatalogUiState.Content -> if (state.data.sections.isEmpty()) {
            MessageState(stringResource(R.string.feature_home_empty), modifier)
        } else {
            HomeContent(feed = state.data, actions = actions, modifier = modifier)
        }
    }
}

@Composable
private fun HomeContent(feed: HomeFeed, actions: CatalogActions, modifier: Modifier = Modifier) {
    val spacing = AuralisTheme.spacing
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = top + spacing.xl, bottom = LocalBottomChromePadding.current + spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        item(key = "greeting") { Greeting() }
        itemsIndexed(feed.sections, key = { index, section -> "$index:${section.title}" }) { _, section ->
            SectionCarousel(section = section, actions = actions)
        }
    }
}

@Composable
private fun Greeting() {
    val greeting = stringResource(
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.feature_home_greeting_morning
            in 12..17 -> R.string.feature_home_greeting_afternoon
            else -> R.string.feature_home_greeting_evening
        },
    )
    Column(
        modifier = Modifier.padding(horizontal = AuralisTheme.spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
    ) {
        Text(
            text = greeting,
            style = AuralisTheme.typography.screenTitle,
            color = AuralisTheme.colors.contentPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.feature_home_subtitle),
            style = AuralisTheme.typography.body,
            color = AuralisTheme.colors.contentSecondary,
        )
    }
}
